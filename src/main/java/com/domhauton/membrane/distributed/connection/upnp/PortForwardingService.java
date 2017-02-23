package com.domhauton.membrane.distributed.connection.upnp;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
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

  private final ScheduledExecutorService scheduledExecutorService;
  private final PortForwardingController portForwardingController;
  private final UpnpService upnpService;

  public PortForwardingService() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    portForwardingController = new PortForwardingController(LEASE_DURATION);
    upnpService = new UpnpServiceImpl();
  }

  @Override
  public void run() {
    upnpService.getRegistry().addListener(portForwardingController);
    upnpService.getControlPoint().search(); // Broadcast search for other devices.
    scheduledExecutorService.scheduleWithFixedDelay(portForwardingController::refreshLeases,
            LEASE_DURATION.toStandardSeconds().getSeconds(),
            LEASE_DURATION.toStandardSeconds().getSeconds(),
            TimeUnit.SECONDS);
  }

  public void close() {
    scheduledExecutorService.shutdown();
  }
}
