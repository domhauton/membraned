package com.domhauton.membrane.network.upnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Period;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class PortForwardingService implements Runnable {
  private static final Logger logger = LogManager.getLogger();
  private static final Period LEASE_DURATION = Period.minutes(5);
  private static final Period GATEWAY_SCAN_FREQUENCY = Period.minutes(1);

  private final ScheduledExecutorService scheduledExecutorService;
  private final PortForwardingController portForwardingController;
  private final int internalPort;

  public PortForwardingService(int internalPort) {
    this(x -> {
    }, internalPort);
  }

  public PortForwardingService(Consumer<ExternalAddress> externalAddressConsumer, int internalPort) {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.internalPort = internalPort;
    portForwardingController = new PortForwardingController(LEASE_DURATION, externalAddressConsumer);
  }

  private ExternalAddress getNonForwardedAddress() {
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface ni = networkInterfaces.nextElement();
        Enumeration<InetAddress> ifaceAddresses = ni.getInetAddresses();
        while (ifaceAddresses.hasMoreElements() && !ni.getDisplayName().startsWith("vmnet")) {
          InetAddress addr = ifaceAddresses.nextElement();
          if (!addr.isLinkLocalAddress() && !addr.isLoopbackAddress() && addr instanceof Inet4Address) {
            return new ExternalAddress(addr.getHostAddress(), internalPort);
          }
        }
      }
    } catch (SocketException e) {
      logger.warn("Unable to detect local address. Returning 192.168.0.1. {}", e.getMessage());
    }
    return new ExternalAddress("192.168.0.1", internalPort);
  }

  public ExternalAddress getExternalAddress() {
    Iterator<ExternalAddress> externalAddressIterator = portForwardingController.getExternalAddresses().iterator();
    return externalAddressIterator.hasNext() ? externalAddressIterator.next() : getNonForwardedAddress();
  }

  public void addNewMapping(int externalPort) {
    portForwardingController.addNATForwardingEntry(internalPort, externalPort);
  }

  @Override
  public void run() {
    scheduledExecutorService.scheduleWithFixedDelay(portForwardingController::refreshLeases,
            LEASE_DURATION.toStandardSeconds().getSeconds()/2,
            LEASE_DURATION.toStandardSeconds().getSeconds()/2,
            TimeUnit.SECONDS);

    scheduledExecutorService.scheduleWithFixedDelay(portForwardingController::discoverDevices,
            0,
            GATEWAY_SCAN_FREQUENCY.toStandardSeconds().getSeconds(),
            TimeUnit.SECONDS);
  }

  public void close() {
    portForwardingController.close();
    scheduledExecutorService.shutdown();
  }
}
