package com.domhauton.membrane.config.items;

import java.io.File;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class DistributedStorageConfig extends StorageConfig {
  private int transportPort;

  public DistributedStorageConfig(String storageFolder, int gcInterval, int softStorageLimit, int hardStorageLimit, int transportPort) {
    super(storageFolder, gcInterval, softStorageLimit, hardStorageLimit);
    this.transportPort = transportPort;
  }

  public DistributedStorageConfig() {
    super(System.getProperty("user.home") + File.separator + ".membrane" + File.separator + "distributed");
    transportPort = 14200;
  }

  public int getTransportPort() {
    return transportPort;
  }
}
