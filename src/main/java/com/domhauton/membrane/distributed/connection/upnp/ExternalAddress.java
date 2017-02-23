package com.domhauton.membrane.distributed.connection.upnp;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class ExternalAddress {
  private final String ipAddress;
  private final int port;

  ExternalAddress(String ipAddress, int port) {
    this.ipAddress = ipAddress;
    this.port = port;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public int getPort() {
    return port;
  }
}
