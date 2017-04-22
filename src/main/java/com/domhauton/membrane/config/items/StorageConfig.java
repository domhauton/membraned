package com.domhauton.membrane.config.items;

import java.io.File;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class StorageConfig {
  private String localShardStorageDir;
  private String peerBlockStorageDir;
  private int gcIntervalMinutes;
  private int storageCapMB;

  public StorageConfig() {
    this.localShardStorageDir = System.getProperty("user.home") + File.separator + ".membrane" + File.separator + "localShards";
    this.peerBlockStorageDir = System.getProperty("user.home") + File.separator + ".membrane" + File.separator + "peerBlocks";
    this.gcIntervalMinutes = 20;
    this.storageCapMB = 4096;
  }

  public StorageConfig(String localShardStorageDir, String peerBlockStorageDir, int gcIntervalMinutes, int storageCapMB) {
    this.localShardStorageDir = localShardStorageDir;
    this.peerBlockStorageDir = peerBlockStorageDir;
    this.gcIntervalMinutes = gcIntervalMinutes;
    this.storageCapMB = storageCapMB;
  }

  public String getLocalShardStorageDir() {
    return localShardStorageDir;
  }

  public String getPeerBlockStorageDir() {
    return peerBlockStorageDir;
  }

  public int getGcIntervalMinutes() {
    return gcIntervalMinutes;
  }

  public int getStorageCapMB() {
    return storageCapMB;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StorageConfig that = (StorageConfig) o;

    return gcIntervalMinutes == that.gcIntervalMinutes &&
        storageCapMB == that.storageCapMB &&
        (localShardStorageDir != null ? localShardStorageDir.equals(that.localShardStorageDir) : that.localShardStorageDir == null);
  }
}
