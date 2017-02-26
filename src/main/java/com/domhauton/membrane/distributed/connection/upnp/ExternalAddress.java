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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExternalAddress that = (ExternalAddress) o;

    if (port != that.port) return false;
    return ipAddress != null ? ipAddress.equals(that.ipAddress) : that.ipAddress == null;
  }

  @Override
  public int hashCode() {
    int result = ipAddress != null ? ipAddress.hashCode() : 0;
    result = 31 * result + port;
    return result;
  }
}
