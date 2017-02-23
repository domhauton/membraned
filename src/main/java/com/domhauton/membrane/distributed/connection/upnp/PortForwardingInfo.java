package com.domhauton.membrane.distributed.connection.upnp;

import org.joda.time.Period;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class PortForwardingInfo {
  private final PortType portType;
  private final int localPort;
  private final int externalPort;
  private final Period timeout;

  PortForwardingInfo(PortType portType, int localPort, int externalPort, Period timeout) {
    this.portType = portType;
    this.localPort = localPort;
    this.externalPort = externalPort;
    this.timeout = timeout;
  }

  PortType getPortType() {
    return portType;
  }

  int getLocalPort() {
    return localPort;
  }

  int getExternalPort() {
    return externalPort;
  }

  Period getTimeout() {
    return timeout;
  }

  @Override
  public String toString() {
    return "PortForwardingInfo{" +
            "portType=" + portType +
            ", localPort=" + localPort +
            ", externalPort=" + externalPort +
            ", timeout=" + timeout +
            '}';
  }

  enum PortType {
    TCP, UDP
  }
}
