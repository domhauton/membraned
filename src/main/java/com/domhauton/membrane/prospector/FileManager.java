package com.domhauton.membrane.prospector;

import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.prospector.metadata.FileMetadata;
import com.domhauton.membrane.prospector.metadata.FileMetadataBuilder;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by dominic on 26/01/17.
 */
public class FileManager {
    private static final int FILE_RESCAN_FREQ_SEC = 10;
    private static final int FOLDER_RESCAN_FREQ_SEC = 120;
    private static final int MAX_CHUNK_SIZE = 1024 * 1024 * 64; // 64MB

    private final Logger logger;
    private final Prospector prospector;
    private final Map<String, FileMetadata> managedFiles;

    private Set<Path> queuedAdditions;

    private final ScheduledExecutorService scanExecutor;
    private final Collection<StorageManager> storageManagers;

    public FileManager() throws FileManagerException {
        logger = LogManager.getLogger();
        try {
            prospector = new Prospector();
        } catch (IOException e) {
            logger.error("Unable to start Prospector.");
            throw new FileManagerException("Failed to start prospector due to IO exception on watcher.");
        }
        this.managedFiles = new HashMap<>();
        storageManagers = new LinkedList<>();

        queuedAdditions = new HashSet<>();

        scanExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void runScanners() {
        runScanners(FILE_RESCAN_FREQ_SEC, FOLDER_RESCAN_FREQ_SEC);
    }

    void runScanners(int fileRescanFrequency, int folderRescanFrequency) {
        scanExecutor.scheduleWithFixedDelay(this::checkFileChanges, 0, fileRescanFrequency, TimeUnit.SECONDS);
        scanExecutor.scheduleWithFixedDelay(this::checkFolderChanges, 0, folderRescanFrequency, TimeUnit.SECONDS);
    }

    void addStorageManager(StorageManager storageManager) {
        storageManagers.add(storageManager);
    }

    public void addWatchFolder(WatchFolder watchFolder) {
        Set<Path> newPaths = prospector.addFolder(watchFolder);
        addExistingFiles(newPaths);
    }

    private void checkFileChanges() {
        ProspectorChangeSet pcs = prospector.checkChanges();
        Set<Path> retryPaths = queuedAdditions;
        queuedAdditions = new HashSet<>();

        retryPaths.forEach(this::addFile);
        pcs.getChangedFiles().forEach(this::addFile);
        pcs.getRemovedFiles().forEach(this::removeFile);
        if(pcs.hasOverflown()) {
            addExistingFiles(prospector.getWatchedFolders());
        }
    }

    void checkFolderChanges() {
        addExistingFiles(prospector.rediscoverFolders());
    }

    private void addExistingFiles(Collection<Path> folders) {
        folders.stream()
                .peek(x -> logger.debug("Adding folder to file manager: [{}]", x))
                .map(Path::toFile)
                .map(File::listFiles)
                .flatMap(Arrays::stream)
                .map(File::toPath)
                .distinct()
                .forEach(this::addFile);
    }

    private void addFile(Path path) {
        File file = path.toFile();
        long lastModified = file.lastModified();
        FileMetadata fileMetadata = managedFiles.getOrDefault(getKey(path, 0), null);
        if(fileMetadata != null && lastModified == fileMetadata.getModifiedTime().getMillis()){
            logger.debug("Update not required. Modify time same for [{}]", path.toString());
        } else {
            fileChanged(path, file);
        }
    }

    private void removeFile(Path path) {
        for(StorageManager sm : storageManagers) {
            sm.removeFile(path, DateTime.now());
        }
    }

    private void fileChanged(Path path, File file) {
        byte[] buffer = new byte[MAX_CHUNK_SIZE];
        DateTime fileLastModified = new DateTime(file.lastModified());
        List<String> shardList = new LinkedList<>();
        logger.trace("File size of [{}] is {}MB", path::toString, () -> ((float) file.length())/(1024*1024));
        boolean hasFileChanged = false;
        try (
                FileInputStream inputStream = new FileInputStream(file)
        ) {
            for(int chunkSize = inputStream.read(buffer); chunkSize != -1; chunkSize = inputStream.read(buffer)) {
                byte[] tailoredArray;
                if(buffer.length != chunkSize) {
                    tailoredArray = Arrays.copyOf(buffer, chunkSize);
                } else {
                    tailoredArray = buffer;
                }
                FileMetadata fileMetadata = new FileMetadataBuilder(path.toString(), shardList.size()+1, tailoredArray)
                        .setModifiedTime(fileLastModified)
                        .build();
                shardList.add(fileMetadata.getStrongHash().toString());
                // Check if chunk has changed
                FileMetadata oldMetaData = managedFiles.getOrDefault(getKey(path, shardList.size()), null);
                boolean hasShardChanged = !fileMetadata.hashCodeEqual(oldMetaData);
                hasFileChanged |= hasShardChanged;
                if (hasShardChanged) {
                    // New shards must be pushed to the storage managers.
                    final int currentChunkSize = chunkSize;
                    logger.trace("Chunk {} from file [{}] of size {}MB sent to storage.",
                            shardList::size,
                            path::toString,
                            () -> ((float) currentChunkSize)/(1024*1024));
                    for(StorageManager sm : storageManagers) {
                        sm.storeShard(fileMetadata.getStrongHash().toString(), tailoredArray);
                    }
                    managedFiles.put(getKey(path, shardList.size()), fileMetadata);
                }
            }
            // If there was a change update the storage managers.
            if(hasFileChanged) {
                logger.info("Change detected in [{}]. Adding file to storage.", path);
                storageManagers.forEach(storageManager -> storageManager.addFile(shardList, fileLastModified, path));
            } else {
                logger.debug("File rescanned but no changed detected [{}].", path);
            }
        } catch (IOException e) {
            logger.error("Error while reading from file [{}]. Ignoring file.", path);
        } catch (StorageManagerException e) {
            queuedAdditions.add(path);
            logger.error("Error while storing shard of file. Read attempt re-queued. [{}]", path);
        }
    }

    private String getKey(Path path, int chunk) {
        return path.toString() + "_" + chunk;
    }
}
