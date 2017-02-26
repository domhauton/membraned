package com.domhauton.membrane.distributed.connection.upnp;

import org.bitlet.weupnp.GatewayDevice;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
    Mockito.when(gatewayDevice.getControlURL()).thenReturn(InetAddress.getLoopbackAddress().getHostAddress());
    Mockito.when(gatewayDevice.getServiceType()).thenReturn("FOOBAR");

    PortForwardingInfo portForwardingInfo = new PortForwardingInfo(PortForwardingInfo.PortType.TCP, 20, 30, Period.minutes(5));
    wanGateway.addPortMapping(portForwardingInfo);
  }
}