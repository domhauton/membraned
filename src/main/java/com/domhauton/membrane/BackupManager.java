package com.domhauton.membrane;

import com.domhauton.membrane.api.RestfulApiException;
import com.domhauton.membrane.api.RestfulApiManager;
import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.network.NetworkException;
import com.domhauton.membrane.network.NetworkManagerImpl;
import com.domhauton.membrane.prospector.FileManager;
import com.domhauton.membrane.prospector.FileManagerException;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.Closeable;
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
public class BackupManager implements Closeable {
  private final static int MB = 1024 * 1024;

  private final Config config;
  private final Path configPath;
  private final DateTime startTime;

  private final FileManager fileManager;
  private final boolean monitorMode;
  private StorageManager localStorageManager;
  private StorageManager distributedStorageManager;
  private RestfulApiManager restfulApiManager;
  private NetworkManagerImpl networkManager;
  private final Logger logger;

  private final ScheduledExecutorService trimExecutor;

  BackupManager(Config config, Path configFilePath) {
    this(config, configFilePath, false);
  }

  BackupManager(Config config, Path configPath, boolean monitorMode) throws IllegalArgumentException {
    logger = LogManager.getLogger();
    this.configPath = configPath;
    this.config = config;
    this.monitorMode = monitorMode;
    this.startTime = DateTime.now();
    try {
      fileManager = new FileManager(config.getWatcher().getChunkSizeMB());
      localStorageManager = new StorageManager(
              Paths.get(config.getLocalStorage().getStorageFolder()),
              config.getLocalStorage().getHardStorageLimit() * MB);
      distributedStorageManager = new StorageManager(
              Paths.get(config.getDistributedStorage().getStorageFolder()),
              config.getDistributedStorage().getHardStorageLimit() * MB);
      if (!monitorMode) {
        fileManager.addStorageManager(localStorageManager);
      }
      trimExecutor = Executors.newSingleThreadScheduledExecutor();
      restfulApiManager = new RestfulApiManager(config.getRest().getPort(), this);
      networkManager = new NetworkManagerImpl(configPath.getParent(), config.getDistributedStorage());
      restfulApiManager.start();
    } catch (FileManagerException | StorageManagerException | RestfulApiException | NetworkException e) {
      logger.error("Failed to start membrane backup manager.");
      logger.error(e.getMessage());
      throw new IllegalArgumentException("Error starting up.", e);
    }
  }

  /**
   * Start backup processes
   */
  void start() {
    loadStorageMappingToProspector();
    loadWatchFoldersToProspector();
    fileManager.runScanners(
            config.getWatcher().getFileRescanInterval(),
            config.getWatcher().getFileRescanInterval());
    if (!monitorMode) { // No need to trim storage in Monitor Mode
      trimExecutor.scheduleWithFixedDelay(this::trimStorage,
              1,
              config.getLocalStorage().getGcInterval(),
              TimeUnit.MINUTES);
    }
  }

  public void recoverFile(Path originalPath, Path destPath) throws StorageManagerException {
    localStorageManager.rebuildFile(originalPath, destPath);
  }

  public void recoverFile(Path originalPath, Path destPath, DateTime atTime) throws StorageManagerException {
    localStorageManager.rebuildFile(originalPath, destPath, atTime);
  }

  void trimStorage() {
    try {
      trimStorageAttempt();
    } catch (StorageManagerException e) {
      logger.error("Failed to trim storage. Trying again in 1 min.");
      trimExecutor.schedule(this::trimStorage, 1, TimeUnit.MINUTES);
    }
  }

  public void trimStorageAttempt() throws StorageManagerException {
    if (monitorMode) {
      logger.warn("Attempted to trim storage in monitor mode!");
    } else {
      long gcBytes = ((long) config.getLocalStorage().getSoftStorageLimit()) * 1024 * 1024;
      Set<Path> watchedFolders = fileManager.getCurrentlyWatchedFolders();
      logger.info("Attempting to trim storage to {}MB.", (float) gcBytes / (1024 * 1024));
      logger.debug("Current watched folders: {}", watchedFolders);
      localStorageManager.clampStorageToSize(gcBytes, watchedFolders);
      logger.info("Successfully trimmed storage.");
    }
  }

  private void loadWatchFoldersToProspector() {
    List<WatchFolder> watchFolders = config.getWatcher().getFolders();
    logger.info("Adding {} watch folders from config to listener", watchFolders.size());
    watchFolders.forEach(fileManager::addWatchFolder);
    fileManager.fullFileScanSweep();
  }

  private void loadStorageMappingToProspector() {
    Map<Path, FileVersion> currentFileMapping = localStorageManager.getCurrentFileMapping();
    logger.info("Moving {} mappings to listener", currentFileMapping.size());
    currentFileMapping.entrySet()
            .forEach(x -> fileManager.addExistingFile(x.getKey(), x.getValue().getModificationDateTime(),
                    x.getValue().getMD5HashLengthPairs()));
  }

  /**
   * Adds a watch folder to the file manager and persists to config
   * @param watchFolder new watchfolder
   * @throws IllegalArgumentException If folder already exists.
   * @throws ConfigException If unable to persist config.
   */
  public void addWatchFolder(WatchFolder watchFolder) throws IllegalArgumentException, ConfigException {
    logger.info("Adding new watch folder");
    if (config.getWatcher().getFolders().contains(watchFolder)) {
      logger.warn("Attempted to add existing watch folder.");
      throw new IllegalArgumentException("Already watching folder!");
    }
    fileManager.addWatchFolder(watchFolder);
    config.getWatcher().getFolders().add(watchFolder);
    ConfigManager.saveConfig(configPath, config);
  }

  public void removeWatchFolder(WatchFolder watchFolder) throws IllegalArgumentException, ConfigException {
    logger.warn("Removing existing watch folder");
    if (!config.getWatcher().getFolders().remove(watchFolder)) {
      throw new IllegalArgumentException("Watch Folder does not exist!");
    }
    fileManager.removeWatchFolder(watchFolder);
    config.getWatcher().getFolders().remove(watchFolder);
    ConfigManager.saveConfig(configPath, config);
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
    return localStorageManager.getCurrentFileMapping().entrySet()
            .stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
  }

  public long getStorageSize() {
    return localStorageManager.getStorageSize();
  }

  public Set<Path> getReferencedFiles() {
    return localStorageManager.getReferencedFiles();
  }

  public List<JournalEntry> getFileHistory(Path filePath) {
    return localStorageManager.getFileHistory(filePath);
  }

  public DateTime getStartTime() {
    return startTime;
  }

  public boolean isMonitorMode() {
    return monitorMode;
  }

  public void close() {
    logger.info("Shutdown - Start");
    if (!monitorMode) {
      logger.info("Shutdown - Stopping Local Storage trimmer.");
      trimExecutor.shutdown();
    }
    try {
      logger.info("Shutdown - Stopping Local Storage.");
      localStorageManager.close();
    } catch (StorageManagerException e) {
      logger.error("Shutdown - Failed to shutdown Local Storage.");
    }

    logger.info("Shutdown - Stopping Distributed Manager");
    networkManager.close();

    try {
      logger.info("Shutdown - Stopping Distributed Storage.");
      distributedStorageManager.close();
    } catch (StorageManagerException e) {
      logger.error("Shutdown - Failed to stop Distributed Storage.");
    }

    logger.info("Shutdown - Stopping Watcher.");
    fileManager.stopScanners();
    logger.info("Shutdown - Stopping Restful Interface.");
    restfulApiManager.close();
    logger.info("Shutdown - Complete");
    LogManager.shutdown();
  }

  /**
   * Registers a shutdown hook for graceful shutdown
   */
  void registerShutdownHook() {
    logger.info("Registering shutdown hook.");
    Runtime.getRuntime().addShutdownHook(new Thread(this::close));
  }
}
