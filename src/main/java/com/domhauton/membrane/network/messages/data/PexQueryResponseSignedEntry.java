package com.domhauton.membrane.network.messages.data;

import org.joda.time.DateTime;

import java.util.Arrays;

/**
 * Created by dominic on 01/04/17.
 */
public class PexQueryResponseSignedEntry extends PexQueryResponseEntry {
  private String userId;
  private boolean isPublic;
  private DateTime dateTime;
  private byte[] signature;

  private PexQueryResponseSignedEntry() {
  } // For Jackson Only

  public PexQueryResponseSignedEntry(String ip, int port, String userId, boolean isPublic, DateTime dateTime, byte[] signature) {
    super(ip, port);
    this.userId = userId;
    this.isPublic = isPublic;
    this.dateTime = dateTime;
    this.signature = signature;
  }

  public String getUserId() {
    return userId;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public DateTime getDateTime() {
    return dateTime;
  }

  public byte[] getSignature() {
    return signature;
  }

  @Override
  public String toString() {
    return "PexQueryResponseSignedEntry{" +
        "ip='" + getIp() + '\'' +
        ", port=" + getPort() +
        ", userId='" + userId + '\'' +
        ", isPublic=" + isPublic +
        ", dateTime=" + dateTime +
        ", signature=" + Arrays.toString(signature) +
        '}';
  }
}
