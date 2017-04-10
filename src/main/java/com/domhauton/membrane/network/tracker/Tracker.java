package com.domhauton.membrane.network.tracker;

/**
 * Created by dominic on 08/04/17.
 */
public class Tracker {
  private final String peerId;
  private final String ip;
  private final int port;

  public Tracker(String peerId, String ip, int port) {
    this.peerId = peerId;
    this.ip = ip;
    this.port = port;
  }

  public String getPeerId() {
    return peerId;
  }

  public String getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }
}
