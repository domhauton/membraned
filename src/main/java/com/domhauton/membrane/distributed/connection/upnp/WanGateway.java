package com.domhauton.membrane.distributed.connection.upnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitlet.weupnp.GatewayDevice;
import org.xml.sax.SAXException;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class WanGateway implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private final GatewayDevice gatewayDevice;
  private final Collection<PortForwardingInfo> mappedPorts;
  private final Consumer<ExternalAddress> externalAddressConsumer;

  WanGateway(GatewayDevice gatewayDevice, Consumer<ExternalAddress> externalAddressConsumer) {
    this.gatewayDevice = gatewayDevice;
    this.mappedPorts = new HashSet<>();
    this.externalAddressConsumer = externalAddressConsumer;
  }

  void addPortMapping(PortForwardingInfo portForwardingInfo) {
    try {
      // Try to add the mapping to the gateway device

      boolean success = addPortMapping(
              portForwardingInfo.getExternalPort(),
              portForwardingInfo.getLocalPort(),
              gatewayDevice.getLocalAddress().getHostAddress(),
              portForwardingInfo.getPortType().toString(),
              "Membrane Transport Mapping",
              portForwardingInfo.getTimeout().toStandardSeconds().getSeconds());

      // If successful save for refresh and ping the callback.

      if(success) {
        mappedPorts.add(portForwardingInfo);
        logger.info("Successfully added port mapping {}:{}->{} on {}",
                portForwardingInfo.getPortType(), portForwardingInfo.getExternalPort(),
                portForwardingInfo.getLocalPort(), gatewayDevice.getFriendlyName());

        // The external address call can error. This could be dealt with more sensibly.

        ExternalAddress externalAddress =
                new ExternalAddress(gatewayDevice.getExternalIPAddress(), portForwardingInfo.getExternalPort());
        externalAddressConsumer.accept(externalAddress);
      } else {
        throw new IOException("Failed to map port");
      }
    } catch (SAXException | IOException e) {
      logger.error("Port mapping on {} failed. {}. Error: {}", gatewayDevice.getFriendlyName(), portForwardingInfo, e.getMessage());
    }
  }

  private void removePortMapping(PortForwardingInfo portForwardingInfo) {
    try {
      boolean success = gatewayDevice.deletePortMapping(portForwardingInfo.getExternalPort(),
              portForwardingInfo.getPortType().toString());
      if(success) {
        mappedPorts.remove(portForwardingInfo);
        logger.info("Successfully removed port mapping {}:{}->{} on {}",
                portForwardingInfo.getPortType(), portForwardingInfo.getExternalPort(),
                portForwardingInfo.getLocalPort(), gatewayDevice.getFriendlyName());
      } else {
        throw new IOException("Failed to unmap port");
      }
    } catch (SAXException | IOException e) {
      logger.error("Port un-mapping on {} failed. {}. Error: {}", gatewayDevice.getFriendlyName(), portForwardingInfo, e.getMessage());
    }
  }

  void refreshAll() {
    mappedPorts.forEach(this::addPortMapping);
  }

  String getFriendlyName() {
    return gatewayDevice.getFriendlyName();
  }

  public void close() {
    mappedPorts.forEach(this::removePortMapping);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WanGateway that = (WanGateway) o;

    return gatewayDevice != null ? gatewayDevice.equals(that.gatewayDevice) : that.gatewayDevice == null;
  }

  @Override
  public int hashCode() {
    return gatewayDevice != null ? gatewayDevice.hashCode() : 0;
  }

  /**
   * Adds a new port mapping to the GatewayDevices using the supplied
   * parameters.
   *
   * @param externalPort   the external associated with the new mapping
   * @param internalPort   the internal port associated with the new mapping
   * @param internalClient the internal client associated with the new mapping
   * @param protocol       the protocol associated with the new mapping
   * @param description    the mapping description
   * @return true if the mapping was successfully added, false otherwise
   * @throws IOException
   * @throws SAXException
   */
  private boolean addPortMapping(int externalPort, int internalPort,
                                String internalClient, String protocol,
                                String description, int leaseDuration)
          throws IOException, SAXException {
    Map<String, String> args = new LinkedHashMap<>();
    args.put("NewRemoteHost", "");    // wildcard, any remote host matches
    args.put("NewExternalPort", Integer.toString(externalPort));
    args.put("NewProtocol", protocol);
    args.put("NewInternalPort", Integer.toString(internalPort));
    args.put("NewInternalClient", internalClient);
    args.put("NewEnabled", Integer.toString(1));
    args.put("NewPortMappingDescription", description);
    args.put("NewLeaseDuration", Integer.toString(leaseDuration));

    Map<String, String> nameValue = GatewayDevice.simpleUPnPcommand(
            gatewayDevice.getControlURL(),
            gatewayDevice.getServiceType(),
            "AddPortMapping",
            args);

    boolean isSuccess = nameValue.get("errorCode") == null;
    if(!isSuccess) {
      logger.trace("Error. Raw return {}", nameValue.toString());
    }
    return isSuccess;
  }
}
