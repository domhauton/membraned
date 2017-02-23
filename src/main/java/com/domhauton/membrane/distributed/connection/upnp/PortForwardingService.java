package com.domhauton.membrane.distributed.connection.upnp;

import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Dominic Hauton on 23/02/17.
 */
public class PortForwardingService implements Runnable {
  private static final Period LEASE_DURATION = new Period(5, PeriodType.minutes());
  private static final Period GATEWAY_SCAN_FREQUENCY = new Period(1, PeriodType.minutes());

  private final ScheduledExecutorService scheduledExecutorService;
  private final PortForwardingController portForwardingController;

  public PortForwardingService() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    portForwardingController = new PortForwardingController(LEASE_DURATION);
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
    scheduledExecutorService.shutdown();
  }
}
