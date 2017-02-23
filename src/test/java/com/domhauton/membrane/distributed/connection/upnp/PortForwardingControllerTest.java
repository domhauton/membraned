package com.domhauton.membrane.distributed.connection.upnp;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
class PortForwardingControllerTest {
  private Period timeoutPeriod = new Period(20, PeriodType.seconds());

  private UpnpService upnpService;
  private PortForwardingController portForwardingController;

  @BeforeEach
  void setUp() throws Exception {
    portForwardingController = new PortForwardingController(timeoutPeriod);
    upnpService = new UpnpServiceImpl(portForwardingController);
  }

  @Test
  void testUpnpRouterDiscovery() throws Exception {
    upnpService.getControlPoint().search();
    Thread.sleep(1000*7);
    System.out.println("this is a test");
  }

  @AfterEach
  void tearDown() throws Exception {
//    Thread.getAllStackTraces().keySet().stream()
//            .filter(thread -> thread.getName().startsWith("cling"))
//            .peek(thread -> System.out.println("Killing cling thread" + thread.getName()))
//            .forEach(Thread::interrupt);
    upnpService.shutdown();
  }
}