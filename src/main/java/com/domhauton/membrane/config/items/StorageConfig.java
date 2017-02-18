package com.domhauton.membrane.config.items;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public abstract class StorageConfig {
  private String storageFolder;
  private int gcInterval;
  private int softStorageLimit;
  private int hardStorageLimit;

  public StorageConfig(String storageFolder) {
    this(storageFolder, 20, 2048, 4096);
  }

  public StorageConfig(String storageFolder, int gcInterval, int softStorageLimit, int hardStorageLimit) {
    this.storageFolder = storageFolder;
    this.gcInterval = gcInterval;
    this.softStorageLimit = softStorageLimit;
    this.hardStorageLimit = hardStorageLimit;
  }

  public String getStorageFolder() {
    return storageFolder;
  }

  public int getGcInterval() {
    return gcInterval;
  }

  public int getSoftStorageLimit() {
    return softStorageLimit;
  }

  public int getHardStorageLimit() {
    return hardStorageLimit;
  }
}
