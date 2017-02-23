package com.domhauton.membrane.distributed.connection.upnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Map;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
class PortForwardingControllerTest {
  private Period timeoutPeriod = new Period(20, PeriodType.seconds());

  private PortForwardingController portForwardingController;

  @BeforeEach
  void setUp() throws Exception {
    portForwardingController = new PortForwardingController(timeoutPeriod);
  }

  @Test
  void testUpnpRouterDiscovery() throws Exception {
    portForwardingController.discoverDevices();
  }

  @Test
  void asrtsra() throws Exception {
    Logger logger = LogManager.getLogger();
    logger.info("Starting weupnp");

    GatewayDiscover discover = new GatewayDiscover();
    logger.info("Looking for Gateway Devices");
    discover.discover();
    Map<InetAddress, GatewayDevice> allGateways = discover.getAllGateways();
    logger.info("Found {} gateways.", allGateways.size());

  }

  @AfterEach
  void tearDown() {
    portForwardingController.close();
  }
}