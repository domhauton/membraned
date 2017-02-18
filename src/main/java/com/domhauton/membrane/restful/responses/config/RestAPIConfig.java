package com.domhauton.membrane.restful.responses.config;

import com.domhauton.membrane.config.items.RestConfig;

/**
 * Created by dominic on 04/02/17.
 */
public class RestAPIConfig {
  private int port;

  public RestAPIConfig(RestConfig restConfig) {
    port = restConfig.getPort();
  }

  public int getPort() {
    return port;
  }
}
