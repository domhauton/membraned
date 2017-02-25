package com.domhauton.membrane.restful.responses;

import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.restful.responses.config.DistributedConfigREST;
import com.domhauton.membrane.restful.responses.config.RestAPIConfig;
import com.domhauton.membrane.restful.responses.config.StorageConfigREST;
import com.domhauton.membrane.restful.responses.config.WatcherConfigREST;

/**
 * Created by dominic on 04/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class MembraneRestConfig implements MembraneResponse {

  private WatcherConfigREST watcher;
  private StorageConfigREST localStorage;
  private DistributedConfigREST distributedStorage;
  private RestAPIConfig restAPI;

  private MembraneRestConfig() {} // Jackson ONLY

  public MembraneRestConfig(Config config) {
    this.restAPI = new RestAPIConfig(config.getRest());
    this.localStorage = new StorageConfigREST(config.getLocalStorage());
    this.distributedStorage = new DistributedConfigREST(config.getDistributedStorage());
    this.watcher = new WatcherConfigREST(config.getWatcher());
  }

  public WatcherConfigREST getWatcher() {
    return watcher;
  }

  public StorageConfigREST getLocalStorage() {
    return localStorage;
  }

  public DistributedConfigREST getDistributedStorage() {
    return distributedStorage;
  }

  public RestAPIConfig getRestAPI() {
    return restAPI;
  }
}
