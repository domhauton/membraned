package com.domhauton.membrane.network.upnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Period;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
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

  public PortForwardingService(Consumer<ExternalAddress> externalAddressConsumer, int internalPort) {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.internalPort = internalPort;
    portForwardingController = new PortForwardingController(LEASE_DURATION, externalAddressConsumer);
  }

  public ExternalAddress getNonForwardedAddress() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      return new ExternalAddress(localHost.getHostAddress(), internalPort);
    } catch (UnknownHostException e) {
      logger.warn("Unable to detect local address. Returning 192.168.0.1");
      return new ExternalAddress("192.168.0.1", internalPort);
    }
  }

  public Set<ExternalAddress> getExternallyMappedAddresses() {
    return portForwardingController.getExternalAddresses();
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
