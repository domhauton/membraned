package com.domhauton.membrane.restful.responses;

import com.domhauton.membrane.restful.responses.config.RestAPIConfig;
import com.domhauton.membrane.restful.responses.config.WatcherConfig;
import com.domhauton.membrane.restful.responses.config.StorageConfig;

/**
 * Created by dominic on 04/02/17.
 */
public class MembraneRestConfig implements MembraneResponse {
    
    private WatcherConfig watcherConfig;
    private StorageConfig storageConfig;
    private RestAPIConfig restAPIConfig;

    public MembraneRestConfig(WatcherConfig watcherConfig, StorageConfig storageConfig, RestAPIConfig restAPIConfig) {
        this.watcherConfig = watcherConfig;
        this.storageConfig = storageConfig;
        this.restAPIConfig = restAPIConfig;
    }

    public WatcherConfig getWatcherConfig() {
        return watcherConfig;
    }

    public StorageConfig getStorageConfig() {
        return storageConfig;
    }

    public RestAPIConfig getRestAPIConfig() {
        return restAPIConfig;
    }
}
