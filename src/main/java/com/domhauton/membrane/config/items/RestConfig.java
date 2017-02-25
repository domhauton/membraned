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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RestConfig that = (RestConfig) o;

    return port == that.port;
  }

  @Override
  public int hashCode() {
    return port;
  }
}
