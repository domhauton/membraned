package com.domhauton.membrane;

import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.prospector.FileManager;
import com.domhauton.membrane.prospector.FileManagerException;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by dominic on 23/01/17.
 */
public class BackupManager {
    private final Config config;
    private final Path configPath;

    private final FileManager fileManager;
    private StorageManager storageManager;
    private Logger logger;

    private final ScheduledExecutorService trimExecutor;

    BackupManager(Config config, Path configPath) throws IllegalArgumentException {
        logger = LogManager.getLogger();
        this.configPath = configPath;
        this.config = config;
        try {
            fileManager = new FileManager(config.getChunkSizeMB());
            storageManager = new StorageManager(Paths.get(config.getShardStorageFolder()), config.getMaxStorageSizeMB() * 1024 * 1024);
            fileManager.addStorageManager(storageManager);
            trimExecutor = Executors.newSingleThreadScheduledExecutor();
        } catch (FileManagerException | StorageManagerException e) {
            logger.error("Failed to start membrane backup manager.");
            logger.error(e.getMessage());
            throw new IllegalArgumentException("Could not start with given config.", e);
        }
    }

    /**
     * Start backup processes
     */
    void start() {
        loadStorageMappingToProspector();
        loadWatchFoldersToProspector();
        fileManager.runScanners(config.getFileRescanFrequencySec(), config.getFolderRescanFrequencySec());
        trimExecutor.scheduleWithFixedDelay(this::trimStorage,
                1,
                config.getStorageTrimFrequencyMin(),
                TimeUnit.MINUTES);
    }

    void recoverFile(Path originalPath, Path destPath) throws StorageManagerException {
        storageManager.rebuildFile(originalPath, destPath);
    }

    void recoverFile(Path originalPath, Path destPath, DateTime atTime) throws StorageManagerException {
        storageManager.rebuildFile(originalPath, destPath, atTime);
    }

    public void trimStorage() {
        long gcBytes = config.getGarbageCollectThresholdMB() * 1024 * 1024;
        Set<Path> watchedFolders = fileManager.getCurrentlyWatchedFolders();
        try {
            logger.info("Attempting to trim storage to {}MB.", config.getGarbageCollectThresholdMB());
            logger.debug("Current watched folders: {}", watchedFolders);
            storageManager.clampStorageToSize(gcBytes, watchedFolders);
            logger.info("Successfully trimmed storage.");
        } catch (StorageManagerException e) {
            logger.error("Failed to trim storage. Trying again in 1 min.");
            trimExecutor.schedule(this::trimStorage, 1, TimeUnit.MINUTES);
        }
    }

    private void loadWatchFoldersToProspector() {
        List<WatchFolder> watchFolders = config.getFolders();
        logger.info("Adding {} watch folders from config to listener", watchFolders.size());
        watchFolders.forEach(fileManager::addWatchFolder);
    }

    private void loadStorageMappingToProspector() {
        Map<Path, FileVersion> currentFileMapping = storageManager.getCurrentFileMapping();
        logger.info("Moving {} mappings to listener", currentFileMapping.size());
        currentFileMapping.entrySet()
                .forEach(x -> fileManager.addExistingFile(x.getKey(), x.getValue().getModificationDateTime(),
                        x.getValue().getMD5HashLengthPairs()));
    }

    public Path getConfigPath() {
        return configPath;
    }

    public Config getConfig() {
        return config;
    }

    public Set<String> getWatchedFolders() {
        return fileManager.getCurrentlyWatchedFolders().stream().map(Path::toString).collect(Collectors.toSet());
    }

    public Set<String> getWatchedFiles() {
        return fileManager.getCurrentlyWatchedFiles();
    }

    public void close() {
        logger.info("Shutdown - Start");
        try {
            logger.info("Shutdown - Saving config.");
            ConfigManager.saveConfig(configPath, config);
        } catch (ConfigException e) {
            logger.error("Failed to save current config.");
        }
        logger.info("Shutdown - Storage trimmer.");
        trimExecutor.shutdown();
        logger.info("Shutdown - Storage Manager.");
        try {
            storageManager.close();
        } catch (StorageManagerException e) {
            logger.error("Shutdown - Failed to shutdown storage manager.");
        }
        logger.info("Shutdown - File Manager.");
        fileManager.stopScanners();
        logger.info("Shutdown - Complete");
    }

    /**
     * Registers a shutdown hook for graceful shutdown
     */
    public void registerShutdownHook() {
        logger.info("Registering shutdown hook.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            close();
            System.exit(1);
        }));
    }


}
