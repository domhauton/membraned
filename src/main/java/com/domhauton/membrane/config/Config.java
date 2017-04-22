package com.domhauton.membrane.config;

import com.domhauton.membrane.config.items.*;

/**
 * Created by dominic on 23/01/17.
 */
@SuppressWarnings("CanBeFinal")
public class Config {
  private ContractManagerConfig contractManager;
  private NetworkConfig network;
  private StorageConfig storage;
  private FileWatcherConfig fileWatcher;
  private RestAPIConfig restApi;

  Config() {
    network = new NetworkConfig();
    storage = new StorageConfig();
    contractManager = new ContractManagerConfig();
    fileWatcher = new FileWatcherConfig();
    restApi = new RestAPIConfig();
  }

  public Config(ContractManagerConfig contractManager, NetworkConfig network, StorageConfig storage, FileWatcherConfig fileWatcher, RestAPIConfig restApi) {
    this.contractManager = contractManager;
    this.network = network;
    this.storage = storage;
    this.fileWatcher = fileWatcher;
    this.restApi = restApi;
  }

  public ContractManagerConfig getContractManager() {
    return contractManager;
  }

  public NetworkConfig getNetwork() {
    return network;
  }

  public StorageConfig getStorage() {
    return storage;
  }

  public FileWatcherConfig getFileWatcher() {
    return fileWatcher;
  }

  public RestAPIConfig getRestApi() {
    return restApi;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Config config = (Config) o;

    return (getContractManager() != null ? getContractManager().equals(config.getContractManager()) : config.getContractManager() == null) &&
        (getNetwork() != null ? getNetwork().equals(config.getNetwork()) : config.getNetwork() == null) &&
        (getStorage() != null ? getStorage().equals(config.getStorage()) : config.getStorage() == null) &&
        (getFileWatcher() != null ? getFileWatcher().equals(config.getFileWatcher()) : config.getFileWatcher() == null) &&
        (getRestApi() != null ? getRestApi().equals(config.getRestApi()) : config.getRestApi() == null);
  }
}
