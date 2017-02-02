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

/**
 * Created by dominic on 23/01/17.
 */
public class BackupManager {
    private ConfigManager configManager;
    private Config config;
    private Path configPath;
    private FileManager fileManager;
    private StorageManager storageManager;
    private Logger logger;

    private final ScheduledExecutorService trimExecutor;

    public BackupManager(Path configPath) {
        logger = LogManager.getLogger();
        this.configPath = configPath;
        configManager = new ConfigManager();
        try {
            config = configPath.toFile().exists() ? configManager.loadConfig(configPath) : configManager.loadDefaultConfig();
            fileManager = new FileManager(config.getChunkSizeMB());
            storageManager = new StorageManager(Paths.get(config.getShardStorageFolder()));
        } catch (ConfigException | FileManagerException | StorageManagerException e) {
            logger.error("Failed to start membrane backup manager.");
            logger.error(e.getMessage());
            System.exit(0);
        }
        trimExecutor = Executors.newSingleThreadScheduledExecutor();
        registerShutdownHook();
    }

    /**
     * Start backup processes
     */
    public void start() {
        loadStorageMappingToProspector();
        loadWatchFoldersToProspector();
        fileManager.runScanners(config.getFileRescanFrequencySec(), config.getFolderRescanFrequencySec());
        trimExecutor.scheduleWithFixedDelay(this::trimStorage,
                1,
                config.getStorageTrimFrequencyMin(),
                TimeUnit.MINUTES);
    }

    private void trimStorage() {
        long maxSizeBytes = config.getMaxStorageSizeMB() * 1024 * 1024;
        Set<Path> watchedFolders = fileManager.getCurrentlyWatchedFolders();
        try {
            logger.info("Attempting to trim storage to {}MB", config.getMaxStorageSizeMB());
            storageManager.clampStorageToSize(maxSizeBytes, watchedFolders);
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
                        x.getValue().getMD5ShardList()));
    }

    /**
     * Registers a shutdown hook for graceful shutdown
     */
    private void registerShutdownHook() {
        logger.info("Registering shutdown hook.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Starting shutdown.\nSaving config.");
            try {
                configManager.saveConfig(configPath, config);
            } catch (ConfigException e) {
                logger.error("Failed to save current config.");
            }
            logger.info("Shutting down storage manager.");
            try {
                storageManager.close();
            } catch (StorageManagerException e) {
                logger.error("Failed to shutdown storage manager.");
            }
            logger.info("Shutting down file manager.");
            fileManager.stopScanners();
            logger.info("Shutdown complete. Exit.");
            System.exit(1);
        }));
    }


}
