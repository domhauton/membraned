package com.domhauton.membrane.distributed.connection;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by Dominic Hauton on 22/02/17.
 */
class UpnpControllerTest {

  private UpnpService upnpService;
  private UpnpController upnpController;

  @BeforeEach
  void setUp() throws Exception {
    upnpController = new UpnpController();
    upnpService = new UpnpServiceImpl(upnpController);
  }

  @Test
  void testUpnpRouterDiscovery() throws Exception {
    upnpService.getControlPoint().search();
    Thread.sleep(1000*4);
  }

  @AfterEach
  void tearDown() {
    upnpService.shutdown();
  }
}