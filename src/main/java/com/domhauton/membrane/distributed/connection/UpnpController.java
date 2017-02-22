package com.domhauton.membrane.distributed.connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.igd.callback.PortMappingDelete;
import org.fourthline.cling.support.model.PortMapping;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Dominic Hauton on 22/02/17.
 */
public class UpnpController extends DefaultRegistryListener {

  private static final Logger logger = LogManager.getLogger();

  private static final DeviceType IGD_DEVICE_TYPE = new UDADeviceType("InternetGatewayDevice", 1);
  private static final DeviceType CONNECTION_DEVICE_TYPE = new UDADeviceType("WANConnectionDevice", 1);

  private static final ServiceType IP_SERVICE_TYPE = new UDAServiceType("WANIPConnection", 1);
  private static final ServiceType PPP_SERVICE_TYPE = new UDAServiceType("WANPPPConnection", 1);

  protected PortMapping[] portMappings;

  // The key of the map is Service and equality is object identity, this is by-design
  private Map<Service, List<PortMapping>> activePortMappings = new HashMap<>();

  UpnpController() {
    super();
  }

  @Override
  synchronized public void deviceAdded(Registry registry, Device device) {

    logger.trace("Discovered new device {}", device.getDisplayString());

    Service connectionService;
    if ((connectionService = discoverConnectionService(device)) == null) return;

    logger.trace("Activating port mappings on: {}", connectionService.getDevice().getDisplayString());

//    final List<PortMapping> activeForService = new ArrayList<>();
//    for (final PortMapping pm : portMappings) {
//      new PortMappingAdd(connectionService, registry.getUpnpService().getControlPoint(), pm) {
//
//        @Override
//        public void success(ActionInvocation invocation) {
//          logger.trace("Port mapping added: ", pm);
//          activeForService.add(pm);
//        }
//
//        @Override
//        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
//          logger.error("Failed to add port mapping: {} Reason: ", pm, defaultMsg);
//        }
//      }.run(); // Synchronous!
//    }
//
//    activePortMappings.put(connectionService, activeForService);
  }

  @Override
  synchronized public void deviceRemoved(Registry registry, Device device) {
    for (Service service : device.findServices()) {
      Iterator<Map.Entry<Service, List<PortMapping>>> it = activePortMappings.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<Service, List<PortMapping>> activeEntry = it.next();
        if (!activeEntry.getKey().equals(service)) continue;

        if (activeEntry.getValue().size() > 0)
          logger.warn("Device disappeared, couldn't delete port mappings: {}", activeEntry.getValue().size());

        it.remove();
      }
    }
  }

  @Override
  synchronized public void beforeShutdown(Registry registry) {
    for (Map.Entry<Service, List<PortMapping>> activeEntry : activePortMappings.entrySet()) {

      final Iterator<PortMapping> it = activeEntry.getValue().iterator();
      while (it.hasNext()) {
        final PortMapping pm = it.next();
        logger.trace("Trying to delete port mapping on IGD: " + pm);
        new PortMappingDelete(activeEntry.getKey(), registry.getUpnpService().getControlPoint(), pm) {

          @Override
          public void success(ActionInvocation invocation) {
            logger.trace("Port mapping deleted: {}", pm);
            it.remove();
          }

          @Override
          public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
            logger.error("Failed to delete port mapping: {} Reason: {}", pm, defaultMsg);
          }

        }.run(); // Synchronous!
      }
    }
  }

  private Service discoverConnectionService(Device device) {
    if (!device.getType().equals(IGD_DEVICE_TYPE)) {
      return null;
    }

    Device[] connectionDevices = device.findDevices(CONNECTION_DEVICE_TYPE);
    if (connectionDevices.length == 0) {
      logger.trace("IGD doesn't support '" + CONNECTION_DEVICE_TYPE + "': " + device);
      return null;
    }

    Device connectionDevice = connectionDevices[0];
    logger.trace("Using first discovered WAN connection device: " + connectionDevice);

    Service ipConnectionService = connectionDevice.findService(IP_SERVICE_TYPE);
    Service pppConnectionService = connectionDevice.findService(PPP_SERVICE_TYPE);

    if (ipConnectionService == null && pppConnectionService == null) {
      logger.trace("IGD doesn't support IP or PPP WAN connection service: " + device);
    }

    return ipConnectionService != null ? ipConnectionService : pppConnectionService;
  }

  public void mapWANPort(int localListeningPort) throws UnknownHostException {
    PortMapping newMapping = new PortMapping(
            localListeningPort,
            InetAddress.getLocalHost().toString(),
            PortMapping.Protocol.TCP,
            "Membrane Transport Port");

    UpnpService upnpService = new UpnpServiceImpl(new PortMappingListener(newMapping));

    upnpService.getControlPoint().search();
  }

}
