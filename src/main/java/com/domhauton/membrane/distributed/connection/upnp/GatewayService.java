package com.domhauton.membrane.distributed.connection.upnp;

import com.offbynull.portmapper.mapper.MappedPort;
import com.offbynull.portmapper.mapper.PortMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Period;

import java.io.Closeable;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class GatewayService implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private PortMapper portMapper;
  private Collection<MappedPort> mappedPorts;

  GatewayService(PortMapper portMapper) {
    this.portMapper = portMapper;
    this.mappedPorts = new HashSet<>();
  }

  void addPortMapping(PortForwardingInfo portForwardingInfo) {
    MappedPort mappedPort = null;
    try {
      mappedPort = portMapper.mapPort(
              portForwardingInfo.getPortType(),
              portForwardingInfo.getLocalPort(),
              portForwardingInfo.getExternalPort(),
              portForwardingInfo.getTimeout().toStandardSeconds().getSeconds());
      mappedPorts.add(mappedPort);
    } catch (InterruptedException e) {
      logger.error("Port mapping on {} failed. {}. Error: {}", portMapper.getSourceAddress(), portForwardingInfo, e.getMessage());
    }
  }

  private void removePortMapping(MappedPort mappedPort) {
    try {
      portMapper.unmapPort(mappedPort);
    } catch (InterruptedException e) {
      logger.error("Port un-mapping on {} failed. {}. Error: {}", portMapper.getSourceAddress(), mappedPort, e.getMessage());
    }
  }

  private void refreshPortMapping(MappedPort mappedPort, Period lifeTime) {
    try {
      portMapper.refreshPort(mappedPort, lifeTime.toStandardSeconds().getSeconds());
    } catch (InterruptedException e) {
      logger.error("Port refresh on {} failed. {}. Error: {}", portMapper.getSourceAddress(), mappedPort, e.getMessage());
    }
  }

  void refreshAll(Period lifeTime) {
    mappedPorts.forEach(mappedPort -> refreshPortMapping(mappedPort, lifeTime));
  }

  InetAddress getAddress() {
    return portMapper.getSourceAddress();
  }

  public void close() {
    mappedPorts.forEach(this::removePortMapping);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GatewayService that = (GatewayService) o;

    return portMapper != null ? portMapper.equals(that.portMapper) : that.portMapper == null;
  }

  @Override
  public int hashCode() {
    return portMapper != null ? portMapper.hashCode() : 0;
  }
}
