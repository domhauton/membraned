package com.domhauton.membrane.config.items;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class RestAPIConfig {
  private int port;

  public RestAPIConfig() {
    this(13200);
  }

  public RestAPIConfig(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RestAPIConfig that = (RestAPIConfig) o;

    return port == that.port;
  }
}
