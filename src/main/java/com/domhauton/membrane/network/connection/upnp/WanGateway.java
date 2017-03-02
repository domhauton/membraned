package com.domhauton.membrane.network.connection.upnp;

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

  /**
   * Sends request to gateway to add mapping.
   *
   * @param portForwardingInfo Port forwarding info to add.
   * @param force              Add mapping to object regardless of reported success
   * @return True if successful
   */
  boolean addPortMapping(PortForwardingInfo portForwardingInfo, boolean force) {
    boolean success = false;
    try {
      // Try to add the mapping to the gateway device

      success = addPortMapping(
              portForwardingInfo.getExternalPort(),
              portForwardingInfo.getLocalPort(),
              gatewayDevice.getLocalAddress().getHostAddress(),
              portForwardingInfo.getPortType().toString(),
              portForwardingInfo.getTimeout().toStandardSeconds().getSeconds());

      // If successful or forced save for refresh and ping the callback.
      if (!success) {
        throw new IOException("Failed to map port");
      }
    } catch (SAXException | IOException e) {
      logger.error("Port mapping on {} failed. {}. Error: {}", gatewayDevice.getFriendlyName(), portForwardingInfo, e.getMessage());
    } finally {
      if (success || force) {
        mappedPorts.add(portForwardingInfo);
        logger.info("Successfully added/refreshed port mapping {}:L{}->R{} on {}",
                portForwardingInfo.getPortType(), portForwardingInfo.getLocalPort(),
                portForwardingInfo.getExternalPort(), gatewayDevice.getFriendlyName());

        try {
          ExternalAddress externalAddress =
                  new ExternalAddress(gatewayDevice.getExternalIPAddress(), portForwardingInfo.getExternalPort());
          externalAddressConsumer.accept(externalAddress);
          success = true;
        } catch (SAXException | IOException e) {
          logger.info("Failed to discover gateway external IP.");
          success = false;
        }
      }
    }
    return success;
  }

  boolean addPortMapping(PortForwardingInfo portForwardingInfo, int attempts) {
    boolean successful = false;
    for(int i = 0; i < attempts && !successful; i++) {
      successful = addPortMapping(portForwardingInfo, false);
      portForwardingInfo = portForwardingInfo.getNextExternalPort();
    }
    return successful;
  }

  void removePortMapping(PortForwardingInfo portForwardingInfo) {
    try {
      boolean success = gatewayDevice.deletePortMapping(portForwardingInfo.getExternalPort(),
              portForwardingInfo.getPortType().toString());
      if(success) {
        mappedPorts.remove(portForwardingInfo);
        logger.info("Successfully removed port mapping {}:L{}->R{} on {}",
                portForwardingInfo.getPortType(), portForwardingInfo.getLocalPort(),
                portForwardingInfo.getExternalPort(), gatewayDevice.getFriendlyName());
      } else {
        throw new IOException("Failed to unmap port");
      }
    } catch (SAXException | IOException e) {
      logger.error("Port un-mapping on {} failed. {}. Error: {}", gatewayDevice.getFriendlyName(), portForwardingInfo, e.getMessage());
    }
  }

  void refreshAll() {
    mappedPorts.forEach(mappedPort -> addPortMapping(mappedPort, false));
  }

  String getFriendlyName() {
    return gatewayDevice.getFriendlyName();
  }

  public void close() {
    logger.info("Removing port mappings for {}", gatewayDevice.getFriendlyName());
    mappedPorts.forEach(this::removePortMapping);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WanGateway that = (WanGateway) o;

    return gatewayDevice != null ? gatewayDevice.getLocalAddress().equals(that.gatewayDevice.getLocalAddress()) : that.gatewayDevice == null;
  }

  @Override
  public int hashCode() {
    return gatewayDevice != null ? gatewayDevice.getLocalAddress().hashCode() : 0;
  }

  /**
   * Adds a new port mapping to the GatewayDevices using the supplied
   * parameters.
   *
   * @param externalPort   the external associated with the new mapping
   * @param internalPort   the internal port associated with the new mapping
   * @param internalClient the internal client associated with the new mapping
   * @param protocol       the protocol associated with the new mapping
   * @return true if the mapping was successfully added, false otherwise
   */
  private boolean addPortMapping(int externalPort, int internalPort, String internalClient,
                                 String protocol, int leaseDuration)
          throws IOException, SAXException {
    Map<String, String> args = new LinkedHashMap<>();
    args.put("NewRemoteHost", "");    // wildcard, any remote host matches
    args.put("NewExternalPort", Integer.toString(externalPort));
    args.put("NewProtocol", protocol);
    args.put("NewInternalPort", Integer.toString(internalPort));
    args.put("NewInternalClient", internalClient);
    args.put("NewEnabled", Integer.toString(1));
    args.put("NewPortMappingDescription", "Membrane Transport Mapping");
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
