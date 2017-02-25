package com.domhauton.membrane.restful.responses.config;

import com.domhauton.membrane.config.items.DistributedStorageConfig;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class DistributedConfigREST extends StorageConfigREST {
  private int transportPort;
  private int externalTransportPort;
  private boolean natForwardingEnabled;

  private DistributedConfigREST() { // Jackson ONLY
    super();
  }

  public DistributedConfigREST(DistributedStorageConfig config) {
    super(config);
    transportPort = config.getTransportPort();
    externalTransportPort = config.getExternalTransportPort();
    natForwardingEnabled = config.isNatForwardingEnabled();
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
