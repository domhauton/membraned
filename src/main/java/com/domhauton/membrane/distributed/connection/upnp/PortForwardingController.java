package com.domhauton.membrane.distributed.connection.upnp;

import com.offbynull.portmapper.PortMapperFactory;
import com.offbynull.portmapper.gateway.Gateway;
import com.offbynull.portmapper.gateways.network.NetworkGateway;
import com.offbynull.portmapper.gateways.network.internalmessages.KillNetworkRequest;
import com.offbynull.portmapper.gateways.process.ProcessGateway;
import com.offbynull.portmapper.gateways.process.internalmessages.KillProcessRequest;
import com.offbynull.portmapper.mapper.PortType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Period;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 22/02/17.
 */
public class PortForwardingController {

  private static final Logger logger = LogManager.getLogger();

  private final Gateway networkGateway;
  private final Gateway processGateway;

  private final LinkedList<PortForwardingInfo> mappings;
  private final Set<GatewayService> gateways;

  private final Period leaseTime;

  PortForwardingController(Period leaseTime) {
    logger.info("Creating port forwarding controller.");
    mappings = new LinkedList<>();
    gateways = new HashSet<>();
    this.leaseTime = leaseTime;

    networkGateway = NetworkGateway.create();
    processGateway = ProcessGateway.create();
    logger.trace("Successfully created forwarding controller.");
  }

  void discoverDevices() {
    try{
      logger.info("Scanning for new gateways.");
      Set<GatewayService> detectedGateways = PortMapperFactory.discover(networkGateway.getBus(), processGateway.getBus())
              .stream()
              .map(GatewayService::new)
              .peek(this::addDevice)
              .collect(Collectors.toSet());

      logger.info("Detected {} gateways during scan.", detectedGateways.size());

//      List<UpnpIgdPortMapper> upnpIgdMappers = UpnpIgdPortMapper.identify(networkGateway.getBus());
//
//      logger.info("Detected {} igd mappers during scan.", upnpIgdMappers.size());


      Set<GatewayService> removedGateways = gateways.stream()
              .filter(gateway -> !detectedGateways.contains(gateway))
              .peek(gatewayService -> logger.info("Lost contact to gateway: {}", gatewayService.getAddress()))
              .collect(Collectors.toSet());

      gateways.removeAll(removedGateways);
    } catch (InterruptedException e) {
      logger.error("Failed to detect new gateway devices: {}", e.getMessage());
    }
  }

  void refreshLeases() {
    gateways.forEach(gatewayService -> gatewayService.refreshAll(leaseTime));
  }

  private void addDevice(GatewayService newGateway) {
    logger.info("Found potential new gateway: {}", newGateway.getAddress());

    if(!gateways.contains(newGateway)) {
      logger.info("Found new gateway: {}", newGateway.getAddress());
      gateways.add(newGateway);
      mappings.forEach(newGateway::addPortMapping);
    }
  }

  public void close() {
    gateways.forEach(GatewayService::close);
    networkGateway.getBus().send(new KillNetworkRequest());
    processGateway.getBus().send(new KillProcessRequest());
  }

  public void addNATForwardingEntry(int localListeningPort, int externalListeningPort) throws UnknownHostException {
    PortForwardingInfo portMapping = new PortForwardingInfo(PortType.TCP, localListeningPort, externalListeningPort, leaseTime);
    mappings.add(portMapping);
    gateways.forEach(gatewayService -> gatewayService.addPortMapping(portMapping));
  }
}
