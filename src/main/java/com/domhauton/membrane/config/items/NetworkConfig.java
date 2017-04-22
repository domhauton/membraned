package com.domhauton.membrane.config.items;

/**
 * Created by dominic on 22/04/17.
 */
public class NetworkConfig {
  private int listeningPort;
  private boolean upnpEnabled;
  private int upnpForwardedPort;
  private int maxConnections;

  public NetworkConfig() {
    listeningPort = 14200;
    upnpEnabled = true;
    upnpForwardedPort = 14200;
    maxConnections = 100;
  }

  public NetworkConfig(int listeningPort, boolean upnpEnabled, int upnpForwardedPort, int maxConnections) {
    this.listeningPort = listeningPort;
    this.upnpEnabled = upnpEnabled;
    this.upnpForwardedPort = upnpForwardedPort;
    this.maxConnections = maxConnections;
  }

  public int getListeningPort() {
    return listeningPort;
  }

  public boolean isUpnpEnabled() {
    return upnpEnabled;
  }

  public int getUpnpForwardedPort() {
    return upnpForwardedPort;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NetworkConfig that = (NetworkConfig) o;

    return getListeningPort() == that.getListeningPort() &&
        isUpnpEnabled() == that.isUpnpEnabled() &&
        getUpnpForwardedPort() == that.getUpnpForwardedPort() &&
        getMaxConnections() == that.getMaxConnections();
  }
}
