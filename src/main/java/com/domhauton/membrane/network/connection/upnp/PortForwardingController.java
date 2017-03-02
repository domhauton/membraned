package com.domhauton.membrane.network.connection.upnp;

import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitlet.weupnp.GatewayDiscover;
import org.joda.time.Period;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 22/02/17.
 */
public class PortForwardingController {

  private static final Logger logger = LogManager.getLogger();

  private final GatewayDiscover discover;

  private final LinkedList<PortForwardingInfo> mappings;
  private final Set<WanGateway> gateways;

  private final Set<ExternalAddress> externalAddresses;
  private final Consumer<ExternalAddress> externalAddressConsumer;

  private final Period leaseTime;

  PortForwardingController(Period leaseTime, Consumer<ExternalAddress> externalAddressConsumer) {
    logger.info("Creating port forwarding controller.");
    mappings = new LinkedList<>();
    gateways = new HashSet<>();
    externalAddresses = new HashSet<>();
    this.leaseTime = leaseTime;
    this.externalAddressConsumer = externalAddressConsumer;

    discover = new GatewayDiscover();
  }

  void discoverDevices() {
    logger.debug("Scanning for new gateways. Currently {} tracked", gateways.size());
    try {
      Set<WanGateway> detectedGateways = discover.discover().values()
              .stream()
              .map(gatewayDevice -> new WanGateway(gatewayDevice, this::addNewExternalAddress))
              .peek(this::addDevice)
              .collect(Collectors.toSet());

      logger.trace("Detected {} gateways during scan.", detectedGateways.size());

      Set<WanGateway> removedGateways = gateways.stream()
              .filter(gateway -> !detectedGateways.contains(gateway))
              .peek(wanGateway -> logger.info("Lost contact to gateway: {}", wanGateway.getFriendlyName()))
              .collect(Collectors.toSet());

      gateways.removeAll(removedGateways);
    } catch (IOException | SAXException | ParserConfigurationException e) {
      logger.error("Failed to scan for new gateways. Error: {}", e.getMessage());
    }
  }

  void refreshLeases() {
    gateways.forEach(WanGateway::refreshAll);
  }

  void addNewExternalAddress(ExternalAddress externalAddress) {
    externalAddresses.add(externalAddress);
    externalAddressConsumer.accept(externalAddress);
  }

  void addDevice(WanGateway newGateway) {
    if (!gateways.contains(newGateway)) {
      logger.info("Found new gateway: {}", newGateway.getFriendlyName());
      gateways.add(newGateway);
      mappings.forEach(mappings -> newGateway.addPortMapping(mappings, 20));
    }
  }

  Set<ExternalAddress> getExternalAddresses() {
    return ImmutableSet.copyOf(externalAddresses);
  }

  public void close() {
    gateways.forEach(WanGateway::close);
  }

  void addNATForwardingEntry(int localListeningPort, int externalListeningPort) {
    PortForwardingInfo portMapping = new PortForwardingInfo(PortForwardingInfo.PortType.TCP, localListeningPort, externalListeningPort, leaseTime);
    mappings.add(portMapping);
    gateways.forEach(wanGateway -> wanGateway.addPortMapping(portMapping, 20));
  }
}
