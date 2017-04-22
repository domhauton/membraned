package com.domhauton.membrane.prospector;

import com.domhauton.membrane.config.items.data.WatchFolder;
import com.domhauton.membrane.prospector.metadata.FileMetadata;
import com.domhauton.membrane.prospector.metadata.FileMetadataBuilder;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageException;
import com.domhauton.membrane.storage.FileEventLogger;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by dominic on 26/01/17.
 */
public class FileManager {
  private final Logger logger;
  private final Prospector prospector;
  private final Map<String, FileMetadata> managedFiles;
  private final int chunkSizeMB;

  private Set<Path> queuedAdditions;

  private final ScheduledExecutorService scanExecutor;
  private FileEventLogger fileEventLogger;
  private ShardStorage shardStorage;

  public FileManager(FileEventLogger fileEventLogger, ShardStorage shardStorage, int chunkSizeMB) throws FileManagerException {
    logger = LogManager.getLogger();
    this.chunkSizeMB = chunkSizeMB;
    try {
      prospector = new Prospector();
    } catch (IOException e) {
      logger.error("Unable to start Prospector.");
      throw new FileManagerException("Failed to start prospector due to IO exception on watcher.");
    }
    this.managedFiles = new HashMap<>();
    this.fileEventLogger = fileEventLogger;
    this.shardStorage = shardStorage;

    queuedAdditions = new HashSet<>();
    scanExecutor = Executors.newSingleThreadScheduledExecutor();
  }

  /**
   * Run file scanner loops at given rates
   *
   * @param fileRescanFrequency   seconds
   * @param folderRescanFrequency seconds
   */
  public void runScanners(int fileRescanFrequency, int folderRescanFrequency) {
    scanExecutor.scheduleWithFixedDelay(this::checkFileChanges, 0, fileRescanFrequency, TimeUnit.SECONDS);
    scanExecutor.scheduleWithFixedDelay(this::fullFileScanSweep, 0, folderRescanFrequency, TimeUnit.SECONDS);
  }

  /**
   * Stop all scanners for graceful shutdown.
   */
  public void stopScanners() {
    scanExecutor.shutdown();
  }

  public void setFileEventLogger(StorageManager fileEventLogger) {
    this.fileEventLogger = fileEventLogger;
  }

  public void setShardStorage(ShardStorage shardStorage) {
    this.shardStorage = shardStorage;
  }

  /**
   * Add file to manager manually.
   *
   * @param path     path of file
   * @param dateTime last modified time of file
   */
  public void addExistingFile(Path path, DateTime dateTime, List<MD5HashLengthPair> md5HashLengthPairs) {
    logger.debug("Adding existing file to file manager: [{}]", path);
    FileMetadata fileMetadata = new FileMetadataBuilder(dateTime)
            .addShardData(md5HashLengthPairs)
            .build();
    managedFiles.put(path.toString(), fileMetadata);
  }

  /**
   * Adds a folder that should be watched.
   */
  public void addWatchFolder(WatchFolder watchFolder) {
    prospector.addWatchFolder(watchFolder);
  }

  /**
   * Adds a folder that should be watched.
   */
  public void removeWatchFolder(WatchFolder watchFolder) {
    prospector.removeWatchFolder(watchFolder);
  }

  /**
   * Scans folders for file changes.
   */
  void checkFileChanges() {
    logger.debug("Checking for File changes.");
    ProspectorChangeSet pcs = prospector.checkChanges();
    Set<Path> retryPaths = queuedAdditions;
    queuedAdditions = new HashSet<>();

    retryPaths.forEach(this::addFile);
    pcs.getChangedFiles().forEach(this::addFile);
    pcs.getRemovedFiles().forEach(this::removeFile);
    if (pcs.hasOverflown()) {
      fullFileScanSweep();
    }
  }

  /**
   * Scans folders for existing files.
   */
  public void fullFileScanSweep() {
    logger.debug("Running full file scan.");
    Collection<Path> folders = prospector.rediscoverFolders();
    Set<Path> existingFiles = folders.stream()
            .peek(x -> logger.debug("Checking folder for existing files: [{}]", x))
            .map(Path::toFile)
            .map(File::listFiles)
            .flatMap(Arrays::stream)
            .map(File::toPath)
            .filter(x -> !Files.isDirectory(x))
            .collect(Collectors.toSet());
    logger.debug("Existing files: {}", existingFiles);
    Set<Path> missingFiles = managedFiles.keySet().stream()
            .map(Paths::get)
            .filter(x -> folders.stream().anyMatch(folder -> x.getParent().equals(folder)))
            .filter(x -> !existingFiles.contains(x))
            .collect(Collectors.toSet());
    Set<Path> orphanedPaths = managedFiles.keySet().stream()
            .filter(x -> folders.stream().noneMatch(folder -> Paths.get(x).getParent().equals(folder)))
            .map(Paths::get)
            .collect(Collectors.toSet());
    orphanedPaths.addAll(missingFiles);
    logger.debug("Lost files: {}", orphanedPaths);
    orphanedPaths.forEach(this::removeFile);
    existingFiles.forEach(this::addFile);
  }

  /**
   * Processes a possibly updated file.
   *
   * @param path path to check
   */
  private void addFile(Path path) {
    logger.info("File Addition Detected: [{}]", path);
    File file = path.toFile();
    long lastModified = file.lastModified();
    FileMetadata fileMetadata = managedFiles.getOrDefault(path.toString(), null);
    if (fileMetadata != null && lastModified == fileMetadata.getModifiedTime().getMillis()) {
      logger.debug("Update not required. Modify time same for [{}]", path.toString());
    } else {
      fileChanged(path);
    }
  }

  /**
   * Notifies storage managers that a file has been removed.
   *
   * @param path path of removed file.
   */
  private void removeFile(Path path) {
    logger.info("File Removal Detected: [{}]", path);
    managedFiles.remove(path.toString());
    try {
      fileEventLogger.removeFile(path, DateTime.now());
    } catch (Exception e) {
      logger.info("File removal failed. Re-queueing.");
      queuedAdditions.add(path);
    }
  }

  /**
   * Checks if the data in a file has been changed and notifies storage managers if true.
   *
   * @param path Path of changed file.
   */
  private void fileChanged(Path path) {
    File file = path.toFile();
    byte[] buffer = new byte[chunkSizeMB * 1024 * 1024];
    DateTime fileLastModified = new DateTime(file.lastModified());
    FileMetadata cachedMetadata = managedFiles.get(path.toString());

    logger.trace("File size of [{}] is {}MB", path::toString, () -> ((float) file.length()) / (1024 * 1024));

    try (
            FileInputStream inputStream = new FileInputStream(file)
    ) {
      FileMetadataBuilder fileMetadataBuilder = new FileMetadataBuilder(fileLastModified);
      for (int chunkSize = inputStream.read(buffer); chunkSize != -1; chunkSize = inputStream.read(buffer)) {
        byte[] tailoredArray;
        if (buffer.length != chunkSize) {
          tailoredArray = Arrays.copyOf(buffer, chunkSize);
        } else {
          tailoredArray = buffer;
        }
        String chunkMD5Hash = fileMetadataBuilder.addHashData(tailoredArray);
        // Check if chunk has changed

        final int currentChunkSize = chunkSize;
        logger.trace("Chunk {} from file [{}] of size {}MB sent to storage.",
                () -> chunkMD5Hash,
                path::toString,
                () -> ((float) currentChunkSize) / (1024 * 1024));
        fileEventLogger.protectShard(chunkMD5Hash);
        shardStorage.storeShard(chunkMD5Hash, tailoredArray);
      }
      FileMetadata newFileMetadata = fileMetadataBuilder.build();

      boolean hasFileChanged = cachedMetadata == null ||
              !cachedMetadata.getModifiedTime().equals(newFileMetadata.getModifiedTime()) ||
              !cachedMetadata.getMd5HashLengthPairs().equals(newFileMetadata.getMd5HashLengthPairs());

      // If there was a change update the storage managers.
      if (hasFileChanged) {
        logger.info("Change detected in [{}]. Adding file to storage.", path);
        fileEventLogger.addFile(newFileMetadata.getMd5HashLengthPairs(), fileLastModified, path);
        managedFiles.put(path.toString(), newFileMetadata);
      } else {
        logger.debug("File rescanned but no changed detected [{}].", path);
      }
    } catch (IOException e) {
      logger.error("Error while reading from file [{}]. Ignoring file.", path);
    } catch (StorageManagerException | ShardStorageException e) {
      queuedAdditions.add(path);
      logger.error("Error while storing shard of file. File re-queued. [{}]", path);
    }
  }

  /**
   * Returns every folder currently watched. Including sub-folders
   */
  public Set<Path> getCurrentlyWatchedFolders() {
    return new HashSet<>(prospector.getWatchedFolders());
  }

  public Set<String> getCurrentlyWatchedFiles() {
    return managedFiles.keySet();
  }
}
