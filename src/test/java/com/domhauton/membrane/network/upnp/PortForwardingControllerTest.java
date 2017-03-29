package com.domhauton.membrane.network.upnp;

import org.joda.time.Period;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

  @Test
  void testUpnpFakeRouter() throws Exception {
    WanGateway wanGateway = Mockito.mock(WanGateway.class);
    portForwardingController.addNATForwardingEntry(32432, 32433);
    portForwardingController.addDevice(wanGateway);
    portForwardingController.refreshLeases();
    Mockito.verify(wanGateway, Mockito.times(1)).addPortMapping(Mockito.any(), Mockito.anyInt());
    Mockito.verify(wanGateway, Mockito.times(1)).refreshAll();
    portForwardingController.close();
    Mockito.verify(wanGateway, Mockito.atLeastOnce()).close();
  }

  @Test
  void testUpnpFakeOldDeviceRemoval() throws Exception {
    WanGateway wanGateway = Mockito.mock(WanGateway.class);
    portForwardingController.addNATForwardingEntry(32432, 32433);
    portForwardingController.addDevice(wanGateway);
    portForwardingController.discoverDevices();
    portForwardingController.refreshLeases();
    Mockito.verify(wanGateway, Mockito.times(1)).addPortMapping(Mockito.any(), Mockito.anyInt());
    Mockito.verify(wanGateway, Mockito.never()).refreshAll();
    portForwardingController.close();
    Mockito.verify(wanGateway, Mockito.never()).close();
  }

  @Test
  void externalAddressCollectionTest() throws Exception {
    portForwardingController.addNewExternalAddress(new ExternalAddress("1.1.1.1", 12345));
    portForwardingController.addNewExternalAddress(new ExternalAddress("1.1.1.2", 12346));
    portForwardingController.addNewExternalAddress(new ExternalAddress("1.1.1.1", 12345));
    Assertions.assertEquals(2, portForwardingController.getExternalAddresses().size());
  }

  @AfterEach
  void tearDown() {
    portForwardingController.close();
  }
}