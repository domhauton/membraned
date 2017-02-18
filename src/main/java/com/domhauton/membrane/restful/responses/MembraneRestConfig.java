package com.domhauton.membrane.restful.responses;

import com.domhauton.membrane.restful.responses.config.RestAPIConfig;
import com.domhauton.membrane.restful.responses.config.StorageConfigREST;
import com.domhauton.membrane.restful.responses.config.WatcherConfigREST;

/**
 * Created by dominic on 04/02/17.
 */
public class MembraneRestConfig implements MembraneResponse {

  private WatcherConfigREST watcherConfig;
  private StorageConfigREST localStorageConfig;
  private StorageConfigREST distributedStorageConfig;
  private RestAPIConfig restAPIConfig;

  public MembraneRestConfig(WatcherConfigREST watcherConfigREST, StorageConfigREST localStorageConfig, StorageConfigREST distributedStorageConfig, RestAPIConfig restAPIConfig) {
    this.watcherConfig = watcherConfigREST;
    this.localStorageConfig = localStorageConfig;
    this.distributedStorageConfig = distributedStorageConfig;
    this.restAPIConfig = restAPIConfig;
  }

  public WatcherConfigREST getWatcher() {
    return watcherConfig;
  }

  public StorageConfigREST getLocalStorage() {
    return localStorageConfig;
  }

  public StorageConfigREST getDistributedStorage() {
    return distributedStorageConfig;
  }

  public RestAPIConfig getRestAPI() {
    return restAPIConfig;
  }
}
