package com.domhauton.membrane.api.responses;

/**
 * Created by dominic on 04/02/17.
 */
public class NetworkStatus implements MembraneResponse {
  private final boolean enabled;
  private final long connectedPeers;
  private final String networkUID;
  private final int maxConnectionCount;
  private final int peerListeningPort;
  private final String upnpAddress;

  public NetworkStatus(boolean enabled, long connectedPeers, String networkUID, int maxConnectionCount, int peerListeningPort, String upnpAddress) {
    this.enabled = enabled;
    this.connectedPeers = connectedPeers;
    this.networkUID = networkUID;
    this.maxConnectionCount = maxConnectionCount;
    this.peerListeningPort = peerListeningPort;
    this.upnpAddress = upnpAddress;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public long getConnectedPeers() {
    return connectedPeers;
  }

  public String getNetworkUID() {
    return networkUID;
  }

  public int getMaxConnectionCount() {
    return maxConnectionCount;
  }

  public int getPeerListeningPort() {
    return peerListeningPort;
  }

  public String getUpnpAddress() {
    return upnpAddress;
  }
}
