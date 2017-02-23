package com.domhauton.membrane.config.items;

import java.io.File;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class DistributedStorageConfig extends StorageConfig {
  private int transportPort;
  private int externalTransportPort;
  private boolean natForwardingEnabled;

  public DistributedStorageConfig(String storageFolder, int gcInterval, int softStorageLimit, int hardStorageLimit, int transportPort, int externalTransportPort, boolean natForwardingEnabled) {
    super(storageFolder, gcInterval, softStorageLimit, hardStorageLimit);
    this.transportPort = transportPort;
    this.externalTransportPort = externalTransportPort;
    this.natForwardingEnabled = natForwardingEnabled;
  }

  public DistributedStorageConfig() {
    super(System.getProperty("user.home") + File.separator + ".membrane" + File.separator + "distributed");
    transportPort = 14200;
    externalTransportPort = 14201;
    natForwardingEnabled = true;
  }

  public int getTransportPort() {
    return transportPort;
  }

  public int getExternalTransportPort() {
    return externalTransportPort;
  }

  public boolean isNatForwardingEnabled() {
    return natForwardingEnabled;
  }
}
