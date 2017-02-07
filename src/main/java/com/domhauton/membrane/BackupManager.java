package com.domhauton.membrane;

import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.prospector.FileManager;
import com.domhauton.membrane.prospector.FileManagerException;
import com.domhauton.membrane.restful.RestfulAPI;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by dominic on 23/01/17.
 */
public class BackupManager {
    private final Config config;
    private final Path configPath;
    private final DateTime startTime;
    private final static String VERSION = "1.0.0-alpha.1";

    private final FileManager fileManager;
    private final boolean monitorMode;
    private StorageManager storageManager;
    private RestfulAPI restfulAPI;
    private Logger logger;

    private final ScheduledExecutorService trimExecutor;

    BackupManager(Config config, Path configPath) {
        this(config, configPath, false);
    }

    BackupManager(Config config, Path configPath, boolean monitorMode) throws IllegalArgumentException {
        logger = LogManager.getLogger();
        this.configPath = configPath;
        this.config = config;
        this.monitorMode = monitorMode;
        this.startTime = DateTime.now();
        try {
            fileManager = new FileManager(config.getChunkSizeMB());
            storageManager = new StorageManager(Paths.get(config.getShardStorageFolder()), config.getMaxStorageSizeMB() * 1024 * 1024);
            if(!monitorMode){
                fileManager.addStorageManager(storageManager);
            }
            trimExecutor = Executors.newSingleThreadScheduledExecutor();
            restfulAPI = new RestfulAPI(config.getVerticlePort(), this);
            restfulAPI.start();
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
        if(monitorMode) {
            trimExecutor.scheduleWithFixedDelay(this::trimStorage,
                    1,
                    config.getStorageTrimFrequencyMin(),
                    TimeUnit.MINUTES);
        }
    }

    public void recoverFile(Path originalPath, Path destPath) throws StorageManagerException {
        storageManager.rebuildFile(originalPath, destPath);
    }

    public void recoverFile(Path originalPath, Path destPath, DateTime atTime) throws StorageManagerException {
        storageManager.rebuildFile(originalPath, destPath, atTime);
    }

    public void trimStorage() {
        try {
            trimStorageAttempt();
        } catch (StorageManagerException e) {
            logger.error("Failed to trim storage. Trying again in 1 min.");
            trimExecutor.schedule(this::trimStorage, 1, TimeUnit.MINUTES);
        }
    }

    public void trimStorageAttempt() throws StorageManagerException {
        if(monitorMode) {
            logger.warn("Attempted to trim storage in monitor mode!");
        } else {
            long gcBytes = ((long) config.getGarbageCollectThresholdMB()) * 1024 * 1024;
            Set<Path> watchedFolders = fileManager.getCurrentlyWatchedFolders();
            logger.info("Attempting to trim storage to {}MB.", (float)gcBytes/(1024*1024));
            logger.debug("Current watched folders: {}", watchedFolders);
            storageManager.clampStorageToSize(gcBytes, watchedFolders);
            logger.info("Successfully trimmed storage.");
        }
    }

    private void loadWatchFoldersToProspector() {
        List<WatchFolder> watchFolders = config.getFolders();
        logger.info("Adding {} watch folders from config to listener", watchFolders.size());
        watchFolders.forEach(fileManager::addWatchFolder);
        fileManager.fullFileScanSweep();
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

    public Set<Path> getCurrentFiles() {
        return storageManager.getCurrentFileMapping().entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public long getStorageSize() {
        return storageManager.getStorageSize();
    }

    public Set<Path> getReferencedFiles() {
        return storageManager.getReferencedFiles();
    }

    public List<JournalEntry> getFileHistory(Path filePath) {
        return storageManager.getFileHistory(filePath);
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public String getVersion() {
        return VERSION;
    }

    public boolean isMonitorMode() {
        return monitorMode;
    }

    public void close() {
        logger.info("Shutdown - Start");
        if(monitorMode) {
            logger.info("Shutdown - Stopping StorageConfig trimmer.");
            trimExecutor.shutdown();
        }
        logger.info("Shutdown - Stopping StorageConfig Manager.");
        try {
            storageManager.close();
        } catch (StorageManagerException e) {
            logger.error("Shutdown - Failed to shutdown storage manager.");
        }
        logger.info("Shutdown - Stopping File Manager.");
        fileManager.stopScanners();
        logger.info("Shutdown - Stopping Restful Interface.");
        restfulAPI.close();
        logger.info("Shutdown - Complete");
    }

    /**
     * Registers a shutdown hook for graceful shutdown
     */
    public void registerShutdownHook() {
        logger.info("Registering shutdown hook.");
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }


}
