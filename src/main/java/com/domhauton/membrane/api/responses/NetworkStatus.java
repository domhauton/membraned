package com.domhauton.membrane.api.responses;

/**
 * Created by dominic on 04/02/17.
 */
public class NetworkStatus implements MembraneResponse {
  private final boolean enabled;
  private final long getConnectedPeers;
  private final String getNetworkUID;
  private final int getMaxConnectionCount;
  private final int getPeerListeningPort;
  private final String getUPnPAddress;

  public NetworkStatus(boolean enabled, long getConnectedPeers, String getNetworkUID, int getMaxConnectionCount, int getPeerListeningPort, String getUPnPAddress) {
    this.enabled = enabled;
    this.getConnectedPeers = getConnectedPeers;
    this.getNetworkUID = getNetworkUID;
    this.getMaxConnectionCount = getMaxConnectionCount;
    this.getPeerListeningPort = getPeerListeningPort;
    this.getUPnPAddress = getUPnPAddress;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public long getGetConnectedPeers() {
    return getConnectedPeers;
  }

  public String getGetNetworkUID() {
    return getNetworkUID;
  }

  public int getGetMaxConnectionCount() {
    return getMaxConnectionCount;
  }

  public int getGetPeerListeningPort() {
    return getPeerListeningPort;
  }

  public String getGetUPnPAddress() {
    return getUPnPAddress;
  }
}
