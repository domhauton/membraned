package com.domhauton.membrane.api.responses.config;

import com.domhauton.membrane.config.items.RestConfig;

/**
 * Created by dominic on 04/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class RestAPIConfig {
  private int port;

  private RestAPIConfig() {} // Jackson ONLY

  public RestAPIConfig(RestConfig restConfig) {
    port = restConfig.getPort();
  }

  public int getPort() {
    return port;
  }
}
