package com.domhauton.membrane.api.responses;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by dominic on 03/02/17.
 */
public class MembraneInfo implements MembraneResponse {
  private String hostname;
  private final String startTime;
  private final int port;
  private final String version;
  private final MembraneStatus status;
  private final String tagline;

  public MembraneInfo(int port, DateTime startTime, String version, MembraneStatus status, String tagline) {
    try {
      this.hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      this.hostname = "unknown";
    }
    this.startTime = startTime.toDateTime(DateTimeZone.UTC).toString(ISODateTimeFormat.dateHourMinuteSecondMillis());
    this.port = port;
    this.version = version;
    this.status = status;
    this.tagline = tagline;
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public String getVersion() {
    return version;
  }

  public String getStartTime() {
    return startTime;
  }

  public MembraneStatus getStatus() {
    return status;
  }

  public String getTagline() {
    return tagline;
  }
}
