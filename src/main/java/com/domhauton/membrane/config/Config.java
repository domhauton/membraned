package com.domhauton.membrane.config;

import com.domhauton.membrane.config.items.DistributedStorageConfig;
import com.domhauton.membrane.config.items.LocalStorageConfig;
import com.domhauton.membrane.config.items.RestConfig;
import com.domhauton.membrane.config.items.WatcherConfig;

/**
 * Created by dominic on 23/01/17.
 */
@SuppressWarnings("CanBeFinal")
public class Config {
  private DistributedStorageConfig distributedStorage;
  private LocalStorageConfig localStorage;
  private WatcherConfig watcher;
  private RestConfig rest;

  public Config() {
    localStorage = new LocalStorageConfig();
    distributedStorage = new DistributedStorageConfig();
    watcher = new WatcherConfig();
    rest = new RestConfig();
  }

  public Config(DistributedStorageConfig distributedStorage, LocalStorageConfig localStorage, WatcherConfig watcher, RestConfig rest) {
    this.distributedStorage = distributedStorage;
    this.localStorage = localStorage;
    this.watcher = watcher;
    this.rest = rest;
  }

  public DistributedStorageConfig getDistributedStorage() {
    return distributedStorage;
  }

  public LocalStorageConfig getLocalStorage() {
    return localStorage;
  }

  public WatcherConfig getWatcher() {
    return watcher;
  }

  public RestConfig getRest() {
    return rest;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Config config = (Config) o;

    if (distributedStorage != null ? !distributedStorage.equals(config.distributedStorage) : config.distributedStorage != null)
      return false;
    if (localStorage != null ? !localStorage.equals(config.localStorage) : config.localStorage != null) return false;
    if (watcher != null ? !watcher.equals(config.watcher) : config.watcher != null) return false;
    return rest != null ? rest.equals(config.rest) : config.rest == null;
  }

  @Override
  public int hashCode() {
    int result = distributedStorage != null ? distributedStorage.hashCode() : 0;
    result = 31 * result + (localStorage != null ? localStorage.hashCode() : 0);
    result = 31 * result + (watcher != null ? watcher.hashCode() : 0);
    result = 31 * result + (rest != null ? rest.hashCode() : 0);
    return result;
  }
}
