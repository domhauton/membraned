package com.domhauton.membrane.prospector;

import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.prospector.metadata.FileMetadata;
import com.domhauton.membrane.prospector.metadata.FileMetadataBuilder;
import com.domhauton.membrane.storage.StorageManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
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
    private static final int CHUNK_SIZE_4MB = 1024 * 1024 * 4; // 4MB

    private final Logger logger;
    private final Prospector prospector;
    private final Map<String, FileMetadata> managedFiles;

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
        scanExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void runScanners() {
        scanExecutor.scheduleWithFixedDelay(this::checkFileChanges, 0, FILE_RESCAN_FREQ_SEC, TimeUnit.SECONDS);
        scanExecutor.scheduleWithFixedDelay(this::checkFolderChanges, 0, FOLDER_RESCAN_FREQ_SEC, TimeUnit.SECONDS);
    }

    public void addWatchfolder(WatchFolder watchFolder) {
        Set<Path> newPaths = prospector.addFolder(watchFolder);
        addExistingFiles(newPaths);
    }

    private void checkFileChanges() {
        prospector.checkChanges()
                .forEach(this::addFile);
    }

    private void checkFolderChanges() {
        prospector.rediscoverFolders()
                .stream()
                .map(Arrays::asList)
                .forEach(this::addExistingFiles);
    }

    private void addExistingFiles(Collection<Path> folders) {
        folders.stream()
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

    private void fileChanged(Path path, File file) {
        byte[] buffer = new byte[CHUNK_SIZE_4MB];
        DateTime fileLastModified = new DateTime(file.lastModified());
        List<String> shardList = new LinkedList<>();
        boolean hasFileChanged = false;
        try (
                FileInputStream inputStream = new FileInputStream(file);
                FileLock lock = inputStream.getChannel().lock() // Need for auto close on exit.
        ) {
            int chunk = 0;
            while (inputStream.read(buffer) != -1) {
                FileMetadata fileMetadata = new FileMetadataBuilder(path.toString(), chunk, buffer)
                        .setModifiedTime(fileLastModified)
                        .build();
                shardList.add(fileMetadata.getStrongHashCode().toString());
                // Check if chunk has changed
                FileMetadata oldMetaData = managedFiles.getOrDefault(getKey(path, chunk), null);
                boolean hasShardChanged = !fileMetadata.hashCodeEqual(oldMetaData);
                hasFileChanged |= hasShardChanged;
                if (hasShardChanged) {
                    // New shards must be pushed to the storage managers.
                    storageManagers.forEach(sm -> sm.storeShard(fileMetadata.getStrongHashCode(), buffer));
                    managedFiles.put(getKey(path, chunk), fileMetadata);
                }
                chunk++;
            }
            // If there was a change update the storage managers.
            if(hasFileChanged) {
                logger.info("Change detected in [{}]. Adding file to storage.", path);
                storageManagers.forEach(storageManager -> storageManager.addFile(shardList, fileLastModified, path));
            } else {
                logger.debug("File rescanned but no changed detected [{}].", path);
            }
        } catch (IOException e) {
            logger.error("Error while reading from file [{}]", path);
        }
    }

    private String getKey(Path path, int chunk) {
        return path.toString() + "_" + chunk;
    }
}
