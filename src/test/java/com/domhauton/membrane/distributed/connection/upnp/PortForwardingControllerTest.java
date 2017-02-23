package com.domhauton.membrane.distributed.connection.upnp;

import org.joda.time.Period;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
class PortForwardingControllerTest {
  private Period timeoutPeriod = Period.seconds(20);

  private PortForwardingController portForwardingController;

  @BeforeEach
  void setUp() throws Exception {
    portForwardingController = new PortForwardingController(timeoutPeriod, externalAddress -> {});
  }

  @Test
  void testUpnpRouterDiscovery() throws Exception {
    portForwardingController.discoverDevices();
    portForwardingController.addNATForwardingEntry(32432, 32433);
  }

  @AfterEach
  void tearDown() {
    portForwardingController.close();
  }
}