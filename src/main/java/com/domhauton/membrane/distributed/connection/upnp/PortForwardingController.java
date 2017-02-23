package com.domhauton.membrane.distributed.connection.upnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.*;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.model.PortMapping;
import org.joda.time.Period;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 22/02/17.
 */
public class PortForwardingController extends DefaultRegistryListener {

  private static final Logger logger = LogManager.getLogger();

  private static final DeviceType IGD_DEVICE_TYPE = new UDADeviceType("InternetGatewayDevice", 1);
  private static final DeviceType CONNECTION_DEVICE_TYPE = new UDADeviceType("WANConnectionDevice", 1);

  private static final ServiceType IP_SERVICE_TYPE = new UDAServiceType("WANIPConnection", 1);
  private static final ServiceType PPP_SERVICE_TYPE = new UDAServiceType("WANPPPConnection", 1);

  private final LinkedList<PortMapping> mappings;
  private final LinkedList<GatewayService> gatewayServices;

  private final Period leaseTime;

  PortForwardingController(Period leaseTime) {
    super();
    mappings = new LinkedList<>();
    gatewayServices = new LinkedList<>();
    this.leaseTime = leaseTime;
  }

  @Override
  synchronized public void deviceAdded(Registry registry, Device device) {

    logger.info("Discovered new gateway device: {}", device.getDisplayString());

    Optional<Service> connectionServiceOptional = discoverConnectionService(device);
    if (connectionServiceOptional.isPresent()) {
      Service connectionService = connectionServiceOptional.get();
      logger.trace("Activating port mappings on: {}", connectionService.getDevice().getDisplayString());
      GatewayService gatewayService = new GatewayService(registry, connectionService);
      gatewayServices.add(gatewayService);

      mappings.forEach(gatewayService::addPortMapping);
    }
  }

  @Override
  synchronized public void deviceRemoved(Registry registry, Device device) {
    Set<Service> gatewayServiceSet = gatewayServices.stream()
            .map(GatewayService::getService)
            .collect(Collectors.toSet());


    Arrays.stream(device.findServices())
            .filter(gatewayServiceSet::contains)
            .forEach(service ->
                    logger.warn("Device disappeared, couldn't delete active port mappings on: {}",
                            service.getDevice().getDisplayString()));
  }

  @Override
  synchronized public void beforeShutdown(Registry registry) {
    gatewayServices.forEach(GatewayService::close);
  }

  private Optional<Service> discoverConnectionService(Device device) {

    Device[] connectionDevices = device.findDevices(CONNECTION_DEVICE_TYPE);

    boolean isIGDevice = device.getType().equals(IGD_DEVICE_TYPE);
    boolean hasConnectionDevice = connectionDevices.length >= 1;

    if (isIGDevice && hasConnectionDevice) {
      Device connectionDevice = connectionDevices[0];
      logger.trace("Using first discovered WAN connection device: " + connectionDevice);

      Service connectionService = connectionDevice.findService(IP_SERVICE_TYPE);
      connectionService = connectionService != null ?
              connectionService : connectionDevice.findService(PPP_SERVICE_TYPE);
      return Optional.ofNullable(connectionService);
    } else {
      return Optional.empty();
    }
  }

  public void addNATForwardingEntry(int localListeningPort, int externalListeningPort) throws UnknownHostException {
    PortMapping portMapping = new PortMapping(true,
            new UnsignedIntegerFourBytes(leaseTime.toStandardSeconds().getSeconds()),
            null,
            new UnsignedIntegerTwoBytes(externalListeningPort),
            new UnsignedIntegerTwoBytes(localListeningPort),
            InetAddress.getLocalHost().toString(),
            PortMapping.Protocol.TCP,
            "Membrane Transport Port");
    mappings.add(portMapping);
    gatewayServices.forEach(gatewayService -> gatewayService.addPortMapping(portMapping));
  }

  void refreshLeases() {
    gatewayServices.forEach(GatewayService::refreshAll);
  }
}
