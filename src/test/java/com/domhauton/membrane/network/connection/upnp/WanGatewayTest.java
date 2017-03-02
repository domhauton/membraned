package com.domhauton.membrane.network.connection.upnp;

import org.bitlet.weupnp.GatewayDevice;
import org.joda.time.Period;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetAddress;
import java.util.function.Consumer;

/**
 * Created by Dominic Hauton on 26/02/17.
 */

class WanGatewayTest {
  @Test
  void simpleAddPortMappingTest() throws Exception {
    GatewayDevice gatewayDevice = Mockito.mock(GatewayDevice.class);
    Consumer<ExternalAddress> consumer = (Consumer<ExternalAddress>) Mockito.mock(Consumer.class);
    WanGateway wanGateway = new WanGateway(gatewayDevice, consumer);

    Mockito.when(gatewayDevice.getFriendlyName()).thenReturn("Mockito Gateway");
    Mockito.when(gatewayDevice.getLocalAddress()).thenReturn(InetAddress.getLoopbackAddress());
    Mockito.when(gatewayDevice.deletePortMapping(Mockito.anyInt(), Mockito.anyString())).thenReturn(true);
    Mockito.when(gatewayDevice.getControlURL()).thenReturn("http://127.0.0.1");
    Mockito.when(gatewayDevice.getServiceType()).thenReturn("FOOBAR");
    Mockito.when(gatewayDevice.getExternalIPAddress()).thenReturn("127.0.5.1");

    PortForwardingInfo portForwardingInfo = new PortForwardingInfo(PortForwardingInfo.PortType.TCP, 20, 30, Period.minutes(5));
    wanGateway.addPortMapping(portForwardingInfo, 2);

    Assertions.assertFalse(wanGateway.addPortMapping(portForwardingInfo, false));
    Assertions.assertTrue(wanGateway.addPortMapping(portForwardingInfo, true));

    Mockito.doThrow(new IOException("Mockito exception")).when(gatewayDevice).getExternalIPAddress();

    Assertions.assertFalse(wanGateway.addPortMapping(portForwardingInfo, true));

    wanGateway.close();
  }

  @Test
  void mockitoGatewayTest() throws Exception {
    GatewayDevice gatewayDevice = Mockito.mock(GatewayDevice.class);
    Consumer<ExternalAddress> consumer = (Consumer<ExternalAddress>) Mockito.mock(Consumer.class);
    WanGateway wanGateway = new WanGateway(gatewayDevice, consumer);

    String friendlyName = "Mockito Gateway";

    Mockito.when(gatewayDevice.getFriendlyName()).thenReturn(friendlyName);
    Mockito.when(gatewayDevice.getLocalAddress()).thenReturn(InetAddress.getLoopbackAddress());
    Mockito.when(gatewayDevice.deletePortMapping(Mockito.anyInt(), Mockito.anyString())).thenReturn(true);
    Mockito.when(gatewayDevice.getControlURL()).thenReturn(InetAddress.getLoopbackAddress().getHostAddress());
    Mockito.when(gatewayDevice.getServiceType()).thenReturn("FOOBAR");

    PortForwardingInfo portForwardingInfo = new PortForwardingInfo(PortForwardingInfo.PortType.TCP, 20, 30, Period.minutes(5));
    wanGateway.removePortMapping(portForwardingInfo);
    Assertions.assertEquals(wanGateway, wanGateway); // Call equals in test...

    Assertions.assertEquals(wanGateway.getFriendlyName(), friendlyName);

    Mockito.when(gatewayDevice.deletePortMapping(Mockito.anyInt(), Mockito.anyString())).thenReturn(false);
    wanGateway.removePortMapping(portForwardingInfo); // Should fail
  }
}