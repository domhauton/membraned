package com.domhauton.membrane;

import com.domhauton.membrane.api.RestfulApiException;
import com.domhauton.membrane.api.RestfulApiManager;
import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.ConfigManager;
import com.domhauton.membrane.config.items.data.WatchFolder;
import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.ContractManagerException;
import com.domhauton.membrane.distributed.ContractManagerImpl;
import com.domhauton.membrane.network.NetworkException;
import com.domhauton.membrane.network.NetworkManagerImpl;
import com.domhauton.membrane.prospector.FileManager;
import com.domhauton.membrane.prospector.FileManagerException;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageImpl;
import com.domhauton.membrane.storage.FileEventLoggerImpl;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
public class BackupManager implements Runnable, Closeable {
  private final static int MB = 1024 * 1024;
  private final static double PEER_LOCAL_STORAGE_RATIO = 0.20d;
  private final static double SOFT_STORAGE_CAP_RATIO = 0.80d;

  private final Config config;
  private final Path configPath;
  private final DateTime startTime;

  private final FileManager fileManager;
  private final boolean monitorMode;
  private StorageManager localStorageManager;
  private RestfulApiManager restfulApiManager;
  private NetworkManagerImpl networkManager;
  private ContractManager contractManager;
  private final Logger logger;

  private final ScheduledExecutorService trimExecutor;
  private ShardStorage localShardStorage;
  private ShardStorage peerBlockStorage;

  BackupManager(Config config, Path configFilePath) {
    this(config, configFilePath, false);
  }

  BackupManager(Config config, Path configPath, boolean monitorMode) throws IllegalArgumentException {
    logger = LogManager.getLogger();
    this.configPath = configPath;
    this.config = config;
    this.monitorMode = monitorMode;
    this.startTime = DateTime.now();
    trimExecutor = Executors.newSingleThreadScheduledExecutor();

    Path configDir = configPath.getParent();
    Path localShardStoragePath = Paths.get(config.getStorage().getLocalShardStorageDir());
    Path peerBlockStoragePath = Paths.get(config.getStorage().getPeerBlockStorageDir());
    localShardStorage = new ShardStorageImpl(
        localShardStoragePath,
        getMaxLocalStorageSize());
    peerBlockStorage = new ShardStorageImpl(
        peerBlockStoragePath,
        getMaxBlockStorageSize(),
        Hashing.sha512());

    try {

      // Create the file manager (responsible for monitoring changes)
      fileManager = new FileManager(new FileEventLoggerImpl(), localShardStorage, config.getFileWatcher().getChunkSizeMB());

      // Create the local storage manager. Responsible for persisting files on the local machine.
      localStorageManager = new StorageManager(configDir, localShardStorage);

      // If not in monitor mode connect the file manager to the storage manager.

      if (!monitorMode) {
        fileManager.setFileEventLogger(localStorageManager);
      }

      // Start the rest API

      restfulApiManager = new RestfulApiManager(config.getRestApi().getPort(), this);
      restfulApiManager.start();

      // Only setup if contract manager is enabled
      if (config.getContractManager().isActive()) {
        // Create in network manager.

        if (config.getNetwork().isUpnpEnabled()) {
          networkManager = new NetworkManagerImpl(
              configDir,
              config.getNetwork().getListeningPort(),
              config.getNetwork().getMaxConnections(),
              config.getNetwork().getUpnpForwardedPort());
        } else {
          networkManager = new NetworkManagerImpl(
              configDir,
              config.getNetwork().getListeningPort(),
              config.getNetwork().getMaxConnections());
        }

        Path contractManagerPath = Paths.get(configDir.toString() + File.separator + "contracts");

        contractManager = new ContractManagerImpl(contractManagerPath,
            localStorageManager,
            localShardStorage,
            peerBlockStorage,
            networkManager,
            config.getContractManager().getTargetContractCount());

        networkManager.setContractManager(contractManager);
        networkManager.setSearchForNewPublicPeers(config.getContractManager().isSearchForNewPeers());
      }

    } catch (FileManagerException | StorageManagerException | RestfulApiException | NetworkException | ContractManagerException e) {
      logger.error("Failed to run membrane backup manager.");
      logger.error(e.getMessage());
      throw new IllegalArgumentException("Error starting up.", e);
    }
  }

  public long getMaxBlockStorageSize() {
    return (long) ((double) config.getStorage().getStorageCapMB() * (double) MB * (1.0 - PEER_LOCAL_STORAGE_RATIO));
  }

  public long getMaxLocalStorageSize() {
    return (long) ((double) config.getStorage().getStorageCapMB() * (double) MB * PEER_LOCAL_STORAGE_RATIO);
  }

  /**
   * Start backup processes
   */
  public void run() {
    loadStorageMappingToProspector();
    loadWatchFoldersToProspector();
    fileManager.runScanners(
        config.getFileWatcher().getFileRescanInterval(),
        config.getFileWatcher().getFileRescanInterval());
    networkManager.run();
    if (!monitorMode) { // No need to trim storage in Monitor Mode
      trimExecutor.scheduleWithFixedDelay(this::trimStorage,
              1,
          config.getStorage().getGcIntervalMinutes(),
              TimeUnit.MINUTES);
    }
    if (contractManager != null) {
      contractManager.run();
    }

    if (networkManager != null) {
      networkManager.run();
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
      long gcSoftLimitBytes = getLocalStorageSoftLimit();
      Set<Path> watchedFolders = fileManager.getCurrentlyWatchedFolders();
      logger.info("Attempting to trim storage to {}MB.", gcSoftLimitBytes / MB);
      logger.debug("Current watched folders: {}", watchedFolders);
      localStorageManager.clampStorageToSize(gcSoftLimitBytes, watchedFolders);
      logger.info("Successfully trimmed storage.");
    }
  }

  public long getLocalStorageSoftLimit() {
    return (long) ((double) getMaxLocalStorageSize() * SOFT_STORAGE_CAP_RATIO);
  }

  private void loadWatchFoldersToProspector() {
    List<WatchFolder> watchFolders = config.getFileWatcher().getFolders();
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
    if (config.getFileWatcher().getFolders().contains(watchFolder)) {
      logger.warn("Attempted to add existing watch folder.");
      throw new IllegalArgumentException("Already watching folder!");
    }
    fileManager.addWatchFolder(watchFolder);
    config.getFileWatcher().getFolders().add(watchFolder);
    ConfigManager.saveConfig(configPath, config);
  }

  public void removeWatchFolder(WatchFolder watchFolder) throws IllegalArgumentException, ConfigException {
    logger.warn("Removing existing watch folder");
    if (!config.getFileWatcher().getFolders().remove(watchFolder)) {
      throw new IllegalArgumentException("Watch Folder does not exist!");
    }
    fileManager.removeWatchFolder(watchFolder);
    config.getFileWatcher().getFolders().remove(watchFolder);
    ConfigManager.saveConfig(configPath, config);
  }

  public Path getConfigPath() {
    return configPath;
  }

  public Config getConfig() {
    return config;
  }

  /* Watcher Info Getters */

  public Set<String> getWatchedFolders() {
    return fileManager.getCurrentlyWatchedFolders().stream().map(Path::toString).collect(Collectors.toSet());
  }

  /* Storage Info Getters */

  public Set<String> getWatchedFiles() {
    return fileManager.getCurrentlyWatchedFiles();
  }

  public Set<Path> getCurrentFiles() {
    return localStorageManager.getCurrentFileMapping().entrySet()
            .stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
  }

  public long getLocalStorageSize() {
    return localShardStorage.getStorageSize();
  }

  public long getPeerStorageSize() {
    return peerBlockStorage.getStorageSize();
  }

  /* Networking Info Getters */

  public boolean isNetworkingEnabled() {
    return networkManager != null;
  }

  public long getConnectedPeers() {
    return networkManager == null ? 0 : networkManager.getConnectedPeers();
  }

  public String getNetworkUID() {
    return networkManager == null ? "n/a" : networkManager.getUID();
  }

  public int getMaxConnectionCount() {
    return networkManager == null ? 0 : config.getNetwork().getMaxConnections();
  }

  public int getPeerListeningPort() {
    return networkManager == null ? 0 : config.getNetwork().getListeningPort();
  }

  public String getUPnPAddress() {
    return networkManager == null || !config.getNetwork().isUpnpEnabled() ?
        "n/a" : networkManager.upnpAddress().toString();
  }

  /* Contract Info Getters */

  public boolean isContractManagerActive() {
    return contractManager != null;
  }

  public int getContractTarget() {
    return contractManager == null ? 0 : config.getContractManager().getTargetContractCount();
  }

  public Set<String> getContractedPeers() {
    return contractManager == null ? Collections.emptySet() : contractManager.getContractedPeers();
  }

  public Set<String> getAllRequiredShards() {
    return contractManager == null ? Collections.emptySet() : localShardStorage.listShardIds();
  }

  public Set<String> getPartiallyDistributedShards() {
    return contractManager == null ? Collections.emptySet() : contractManager.getPartiallyDistributedShards();
  }

  public Set<String> getFullyDistributedShards() {
    return contractManager == null ? Collections.emptySet() : contractManager.getFullyDistributedShards();
  }

  /* Assorted Getters */

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

    if (networkManager != null) {
      logger.info("Shutdown - Stopping Network Manager");
      networkManager.close();
    }

    if (contractManager != null) {
      logger.info("Shutdown - Stopping Contract Manager");
      contractManager.close();
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
