package com.domhauton.membrane.distributed.connection.upnp;

import org.joda.time.Period;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class PortForwardingService implements Runnable {
  private static final Period LEASE_DURATION = Period.minutes(5);
  private static final Period GATEWAY_SCAN_FREQUENCY = Period.minutes(1);

  private final ScheduledExecutorService scheduledExecutorService;
  private final PortForwardingController portForwardingController;

  public PortForwardingService(Consumer<ExternalAddress> externalAddressConsumer) {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    portForwardingController = new PortForwardingController(LEASE_DURATION, externalAddressConsumer);
  }

  public Set<ExternalAddress> getExternallyMappedAddresses() {
    return portForwardingController.getExternalAddresses();
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
