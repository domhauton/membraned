package com.domhauton.membrane.network.messages.data;

/**
 * Created by dominic on 01/04/17.
 */
public class PexQueryResponseEntry {
  private String ip;
  private int port;

  PexQueryResponseEntry() {
  } // For Jackson Only

  public PexQueryResponseEntry(String ip, int port) {
    this.ip = ip;
    this.port = port;
  }

  public String getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }

  @Override
  public String toString() {
    return "PexQueryResponseEntry{" +
        "ip='" + ip + '\'' +
        ", port=" + port +
        '}';
  }
}
