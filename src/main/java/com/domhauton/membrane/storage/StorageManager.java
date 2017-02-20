package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.catalogue.CatalogueUtils;
import com.domhauton.membrane.storage.catalogue.FileCatalogue;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import com.domhauton.membrane.storage.shard.ShardStorage;
import com.domhauton.membrane.storage.shard.ShardStorageException;
import com.domhauton.membrane.storage.shard.ShardStorageImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 * <p>
 * Manages sharded file storage.
 */
public class StorageManager {
  static final String DEFAULT_STORAGE_FOLDER = "data";
  static final String DEFAULT_CATALOGUE_FOLDER = "catalogue";
  static final String JOURNAL_NAME = "journal.csv";
  private static final String BASE_FILE_MAP_NAME = "file-map.csv";

  private final Logger logger;
  private final ShardStorage shardStorage;
  private FileCatalogue fileCatalogue;
  private final Set<String> tempProtectedShards;

  private Path baseFileMapPath;
  private final Path journalPath;

  private OutputStreamWriter journalOutput;

  public StorageManager(Path basePath, int maxStorageSize) throws StorageManagerException {
    this(
            Paths.get(basePath.toString() + File.separator + DEFAULT_CATALOGUE_FOLDER),
            Paths.get(basePath.toString() + File.separator + DEFAULT_STORAGE_FOLDER), maxStorageSize);
  }

  private StorageManager(Path catalogue, Path data, long maxStorageSize) throws StorageManagerException {
    logger = LogManager.getLogger();
    logger.info("Opening storage manager.");
    shardStorage = new ShardStorageImpl(data, maxStorageSize);
    tempProtectedShards = new HashSet<>();
    journalPath = Paths.get(catalogue.toString() + File.separator + JOURNAL_NAME);
    List<JournalEntry> journalEntries = journalPath.toFile().exists() ? readJournal(journalPath) : new LinkedList<>();
    baseFileMapPath = Paths.get(catalogue.toString() + File.separator + BASE_FILE_MAP_NAME);
    Map<Path, FileVersion> fileMap = baseFileMapPath.toFile().exists() ? readFileMap(baseFileMapPath) : new HashMap<>();
    journalOutput = openJournalOutputStream(journalPath);
    fileCatalogue = new FileCatalogue(fileMap, journalEntries);
  }

  /**
   * Persist the given shard in the storage manager
   *
   * @param md5Hash the MD5 has of the data.
   * @param data    The data to persist
   * @throws StorageManagerException If there is a problem writing the data.
   */
  public void storeShard(String md5Hash, byte[] data) throws StorageManagerException {
    try {
      logger.info("Storing shard [{}]", md5Hash);
      tempProtectedShards.add(md5Hash);
      shardStorage.storeShard(md5Hash, data);
    } catch (ShardStorageException e) {
      throw new StorageManagerException(e.getMessage());
    }
  }

  /**
   * Add a file to be stored
   *
   * @param shardHash            All of the file's shards in order of reconstruction
   * @param modificationDateTime Time the modification occurred.
   * @param storedPath           The actual path of the file.
   */
  public synchronized void addFile(List<MD5HashLengthPair> shardHash, DateTime modificationDateTime, Path storedPath) throws StorageManagerException {
    logger.info("Adding file [{}] - Timestamp [{}] - Shards [{}]", storedPath, modificationDateTime, shardHash);
    try {
      fileCatalogue.addFile(shardHash, modificationDateTime, storedPath, journalOutput);
    } catch (IOException e) {
      throw new StorageManagerException("Failed to write update to journal.");
    }
  }

  /**
   * Indicates the file was removed.
   *
   * @param storedPath           The path of the stored file
   * @param modificationDateTime The time the removal occurred.
   */
  public synchronized void removeFile(Path storedPath, DateTime modificationDateTime) {
    logger.info("Removing file [{}] - Time: {}", storedPath, modificationDateTime);
    fileCatalogue.removeFile(storedPath, modificationDateTime);
  }


  /**
   * Rebuilds the given file at the given destination.
   *
   * @param originalPath file to recover
   * @param destPath     where to write recovered file to
   * @param atTime       recovers the file at the given time
   * @throws StorageManagerException If there's a problem reading the shards, finding metadata or writing result.
   */
  public void rebuildFile(Path originalPath, Path destPath, DateTime atTime) throws StorageManagerException {
    Optional<FileVersion> fileVersionOptional = fileCatalogue.getFileVersion(originalPath, atTime);
    if (fileVersionOptional.isPresent()) {
      rebuildFile(originalPath, destPath, fileVersionOptional.get());
    } else {
      logger.error("Rebuilding file [{}] - Failed to locate file metadata for file at time {}.", originalPath, atTime);
      throw new StorageManagerException("File unknown. [" + originalPath + "]");
    }
  }

  /**
   * Rebuilds the given file at the given destination.
   *
   * @param originalPath file to recover
   * @param destPath     where to write recovered file to
   * @throws StorageManagerException If there's a problem reading the shards, finding metadata or writing result.
   */
  public void rebuildFile(Path originalPath, Path destPath) throws StorageManagerException {
    Optional<FileVersion> fileVersionOptional = fileCatalogue.getFileVersion(originalPath);
    if (fileVersionOptional.isPresent()) {
      rebuildFile(originalPath, destPath, fileVersionOptional.get());
    } else {
      logger.error("Rebuilding file [{}] - Failed to locate file metadata.", originalPath);
      throw new StorageManagerException("File unknown. [" + originalPath + "]");
    }
  }

  /**
   * Rebuilds the given file at the given destination.
   *
   * @param originalPath file to recover
   * @param destPath     where to write recovered file to
   * @param fileVersion  using the given fileVersion
   * @throws StorageManagerException If there's a problem reading the shards, finding metadata or writing result.
   */
  private void rebuildFile(Path originalPath, Path destPath, FileVersion fileVersion) throws StorageManagerException {
    logger.info("Rebuilding file [{}] - Destination [{}]", originalPath, destPath);
    if(destPath.toFile().exists()) {
      logger.warn("Asked to reconstruct onto existing file [{}]. Stopping.", destPath);
      throw new StorageManagerException("Asked to reconstruct onto existing file. Please delete first.");
    }
    try (
            FileOutputStream fos = new FileOutputStream(destPath.toFile());
            BufferedOutputStream bus = new BufferedOutputStream(fos)
    ) {
      for (MD5HashLengthPair MD5HashLengthPair : fileVersion.getMD5HashLengthPairs()) {
        logger.info("Rebuilding file [{}] - Shard [{}]. Size: {}", originalPath, MD5HashLengthPair.getMd5Hash(), MD5HashLengthPair.getLength());
        byte[] data = shardStorage.retrieveShard(MD5HashLengthPair.getMd5Hash());
        bus.write(data);
      }
      logger.info("Rebuilding file [{}] - SUCCESS.", originalPath);
    } catch (ShardStorageException e) {
      try {
        Files.delete(destPath);
      } catch (IOException e1) {
        logger.error("Rebuilding file [{}] - Failed to remove partially reconstructed file.", originalPath);
      }
      throw new StorageManagerException("Shard missing. Reconstruction failed. " + e.getMessage());
    } catch (FileNotFoundException e) {
      logger.error("Rebuilding file [{}] - Failed to locate file shard.", originalPath);
      throw new StorageManagerException("Could not find output file. " + e.getMessage());
    } catch (IOException e) {
      logger.error("Rebuilding file [{}] - Failed to write to output file. [{}]", originalPath, destPath);
      throw new StorageManagerException("Could not write to output file. " + e.getMessage());
    }

  }

  /**
   * Removes any shards not referenced in the catalogue from the storage.
   */
  long collectGarbage() {
    logger.info("Garbage collection - Start");
    Set<String> requiredShards = fileCatalogue.getReferencedShards();
    Set<String> garbageShards = shardStorage.listShards();
    garbageShards.removeAll(requiredShards);
    garbageShards.removeAll(tempProtectedShards);
    logger.info("Garbage collection - Found {} de-referenced shards", garbageShards.size());
    AtomicLong removedSize = new AtomicLong(0L);
    garbageShards.stream()
            .peek(x -> logger.info("Garbage collection - Removing shard: [{}]", x))
            .forEach(x -> {
              try {
                long fileSize = shardStorage.removeShard(x);
                removedSize.addAndGet(fileSize);
              } catch (ShardStorageException e) {
                // Ignore - It doesn't exist already
              }
            });
    logger.info("Garbage collection - Complete - Removed {}MB", ((float) removedSize.get()) / (1024 * 1024));
    return removedSize.get();
  }

  public synchronized long clampStorageToSize(long bytes, Set<Path> trackedFolders) throws StorageManagerException {
    long currentStorageSize = getStorageSize();
    long spaceToRecover = currentStorageSize - bytes;
    logger.info("Space Recovery - Reducing storage to {}MB. Current size {}MB. Need to remove {}MB", ((float) bytes) / (1024 * 1024), ((float) currentStorageSize) / (1024 * 1024), ((float) Math.max(spaceToRecover, 0)) / (1024 * 1024));
    if (spaceToRecover > 0) {
      logger.info("Space Recovery - Collecting unnecessary shards.");
      spaceToRecover -= collectGarbage();
    }

    if (spaceToRecover > 0) {
      logger.info("Space Recovery - Finding un-tracked files.");
      Set<Path> notTrackedFiles = fileCatalogue.getCurrentFiles().stream()
              .filter(x -> !trackedFolders.contains(x.getParent()))
              .collect(Collectors.toSet());
      if (notTrackedFiles.size() > 0) {
        logger.info("Space Recovery - Removing {} un-tracked files.", notTrackedFiles.size());
        notTrackedFiles.forEach(x -> fileCatalogue.forgetFile(x));
        spaceToRecover -= cleanStorage(fileCatalogue.getOldestJournalEntryTime());
      } else {
        logger.info("Space Recovery - No un-tracked files found.", notTrackedFiles.size());
      }
    }

    if (spaceToRecover > 0) {
      logger.info("Space Recovery - Retiring older journal entries.");
      fileCatalogue.removeOldestJournalEntries((int) spaceToRecover);
      spaceToRecover -= cleanStorage(fileCatalogue.getOldestJournalEntryTime());
    }

    if (spaceToRecover > 0) {
      logger.warn("Space Recovery - Could not meet {}MB requirement. Excess is {}MB.", ((float) bytes) / (1024 * 1024), ((float) spaceToRecover) / (1024 * 1024));
    } else {
      logger.info("Space Recovery - Successful file size reduction. Reduced to {}MB.", ((float) (bytes + spaceToRecover)) / (1024 * 1024));
    }
    return spaceToRecover;
  }

  public long getStorageSize() {
    return shardStorage.getStorageSize();
  }

  /**
   * Removes journal entries and files up to the given point
   *
   * @param moveTo shift knowledge to date
   * @return bytes saved by pushing forward
   */
  public synchronized long cleanStorage(DateTime moveTo) throws StorageManagerException {
    fileCatalogue = fileCatalogue.cleanCatalogue(moveTo);
    String dtString = DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMdd-HHmmss"));
    try {
      logger.info("Clean StorageConfigREST - Closing journal temporarily. [{}}", journalPath);
      journalOutput.close();

      if (journalPath.toFile().exists()) {
        Path backupJournalPath = Paths.get(journalPath.toString() + ".bkp." + dtString);
        logger.info("Clean StorageConfigREST - Journal backup to [{}]", backupJournalPath);
        Files.deleteIfExists(backupJournalPath);
        Files.copy(journalPath, backupJournalPath);
        Files.delete(journalPath);
        Files.createFile(journalPath);
      }

      if (baseFileMapPath.toFile().exists()) {
        Path baseFileMapBackupPath = Paths.get(baseFileMapPath.toString() + ".bkp." + dtString);
        logger.info("Clean StorageConfigREST - Base File Map backup to [{}]", baseFileMapBackupPath);
        Files.deleteIfExists(baseFileMapBackupPath);
        Files.move(baseFileMapPath, baseFileMapBackupPath);
      }

      List<String> newJournalEntries = fileCatalogue.getFullJournal().stream()
              .map(JournalEntry::toString)
              .map(s -> s + "\n")
              .collect(Collectors.toList());

      logger.info("Clean StorageConfigREST - Writing {} entries to new journal to [{}]", newJournalEntries.size(), journalPath);
      logger.trace("Writing entries: {}", () -> newJournalEntries);
      Files.write(journalPath, newJournalEntries);
      logger.info("Clean StorageConfigREST - Writing new base file map to [{}]", baseFileMapPath);
      Files.write(baseFileMapPath, fileCatalogue.serializeBaseMap());
      logger.info("Clean StorageConfigREST - Reopening journal. [{}]", journalPath);
      journalOutput = openJournalOutputStream(journalPath);
    } catch (IOException e) {
      logger.error("Clean StorageConfigREST - There was an IOException while cleaning storage! Might need to manually recover!");
      throw new StorageManagerException("IOException while cleaning storage: " + e.toString());
    }
    return collectGarbage();
  }

  /**
   * Remove shards protected.
   * Should only be done when there are no shards added that have not been referenced yet
   */
  void clearProtectedShards() {
    tempProtectedShards.clear();
  }

  /**
   * Returns the most recently known file-shard mapping.
   */
  public Map<Path, FileVersion> getCurrentFileMapping() {
    return fileCatalogue.getCurrentFileMappings();
  }

  public Set<Path> getReferencedFiles() {
    return fileCatalogue.getReferencedFiles();
  }

  /**
   * Read journal from given path.
   */
  private List<JournalEntry> readJournal(Path journalPath) throws StorageManagerException {
    try {
      List<String> journalEntries = Files.readAllLines(journalPath);
      logger.trace("Loaded following journalEntries: {}", () -> journalEntries);
      List<JournalEntry> convertedJournalEntries = new LinkedList<>();
      for (String fileEntry : journalEntries) {
        if (!fileEntry.isEmpty()) {
          logger.debug("Parsing journal: {}", fileEntry);
          JournalEntry entry = new JournalEntry(fileEntry);
          convertedJournalEntries.add(entry);
        }
      }
      logger.info("Loaded {} entries from journal at [{}]", convertedJournalEntries.size(), journalPath);
      return convertedJournalEntries;
    } catch (IllegalArgumentException | IOException e) {
      logger.error("Could not read from journal at {}", journalPath);
      throw new StorageManagerException("Could not read journal");
    }
  }

  /**
   * Read file map from given path
   */
  private Map<Path, FileVersion> readFileMap(Path fileMapPath) throws StorageManagerException {
    try {
      List<String> fileMapEntries = Files.readAllLines(fileMapPath);
      logger.trace("Loaded following file map: {}", () -> fileMapEntries);
      Map<Path, FileVersion> retMap = CatalogueUtils.generateInputMap(fileMapEntries);
      logger.info("Loaded {} entries from file map at [{}]", retMap.size(), fileMapPath);
      return retMap;
    } catch (IllegalArgumentException | IOException e) {
      logger.error("Could not read from journal at {}", fileMapPath);
      throw new StorageManagerException("Could not read filemap: " + fileMapPath);
    }
  }

  /**
   * Opens the journal path for the given output stream.
   */
  private OutputStreamWriter openJournalOutputStream(Path journalPath) throws StorageManagerException {
    try {
      if (!journalPath.toFile().exists()) {
        boolean created = journalPath.toFile().getParentFile().mkdirs();
        if (created) {
          logger.debug("Open Journal - [{}] Created dirs for journal. ", journalPath);
        }
        Files.createFile(journalPath);
        logger.debug("Open Journal - [{}] Created journal file", journalPath);
      }
      logger.debug("Open Journal - [{}]", journalPath);
      return new OutputStreamWriter(new FileOutputStream(journalPath.toFile(), true));
    } catch (IOException e) {
      logger.error("Open Journal - Could not open journal for writing at {}", journalPath);
      throw new StorageManagerException("Could not open journal for writing. " + journalPath);
    }
  }

  public List<JournalEntry> getFileHistory(Path path) {
    return fileCatalogue.getFileVersionHistory(path);
  }

  /**
   * Stops the storage manager
   *
   * @throws StorageManagerException failed to flush everything from memory to disk
   */
  public synchronized void close() throws StorageManagerException {
    logger.info("Closing storage manager.");
    try {
      journalOutput.close();
    } catch (IOException e) {
      logger.error("Could not flush journal output!");
      throw new StorageManagerException("Could not flush journal output!");
    }
  }
}
