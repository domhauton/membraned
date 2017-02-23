package com.domhauton.membrane.distributed.connection.upnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.igd.callback.PortMappingDelete;
import org.fourthline.cling.support.model.PortMapping;

import java.io.Closeable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class GatewayService implements Closeable {
  private static final Logger logger = LogManager.getLogger();

  private Registry registry;
  private Service service;
  private Collection<PortMapping> portMappings;

  GatewayService(Registry registry, Service service) {
    this.registry = registry;
    this.service = service;
    this.portMappings = new HashSet<>();
  }

  void addPortMapping(PortMapping portMapping) {
    PortMappingAdd newPortMapping = new PortMappingAdd(service, registry.getUpnpService().getControlPoint(), portMapping) {
      @Override
      public void success(ActionInvocation invocation) {
        logger.trace("Port mapping added: ", portMapping);
        portMappings.add(portMapping);
      }

      @Override
      public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        logger.error("Failed to add port mapping: {} Reason: ", portMapping, defaultMsg);
      }
    };
    newPortMapping.run();
  }

  private void removePortMapping(PortMapping portMapping) {
    PortMappingDelete portMappingDelete = new PortMappingDelete(service, registry.getUpnpService().getControlPoint(), portMapping) {
      @Override
      public void success(ActionInvocation invocation) {
        logger.trace("Port mapping deleted: {}", portMapping);
      }

      @Override
      public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        logger.error("Failed to delete port mapping: {} Reason: {}", portMapping, defaultMsg);
      }
    };
    portMappingDelete.run();
  }

  void refreshAll() {
    portMappings.forEach(this::addPortMapping);
  }

  Service getService() {
    return service;
  }

  public void close() {
    portMappings.forEach(this::removePortMapping);
  }
}
