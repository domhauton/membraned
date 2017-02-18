package com.domhauton.membrane.config.items;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class RestConfig {
  private int port;

  public RestConfig() {
    this(13200);
  }

  public RestConfig(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }
}
