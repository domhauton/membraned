package com.domhauton.membrane.restful.responses.config;

/**
 * Created by dominic on 04/02/17.
 */
public class RestAPIConfig {
  private int port;

  public RestAPIConfig(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }
}
