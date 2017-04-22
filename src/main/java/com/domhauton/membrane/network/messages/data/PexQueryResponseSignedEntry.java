package com.domhauton.membrane.network.messages.data;

import org.bouncycastle.util.encoders.Base64;
import org.joda.time.DateTime;

/**
 * Created by dominic on 01/04/17.
 */
public class PexQueryResponseSignedEntry extends PexQueryResponseEntry {
  private String userId;
  private boolean publicEntry;
  private long dateTimeMillis;
  private byte[] signature;

  private PexQueryResponseSignedEntry() {
  } // For Jackson Only

  public PexQueryResponseSignedEntry(String ip, int port, String userId, boolean publicEntry, DateTime dateTime, byte[] signature) {
    super(ip, port);
    this.userId = userId;
    this.publicEntry = publicEntry;
    this.dateTimeMillis = dateTime.getMillis();
    this.signature = signature;
  }

  public String getUserId() {
    return userId;
  }

  public boolean isPublicEntry() {
    return publicEntry;
  }

  public long getDateTimeMillis() {
    return dateTimeMillis;
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
        ", isPublicInfo=" + publicEntry +
        ", dateTimeMillis=" + dateTimeMillis +
        ", signature=" + Base64.toBase64String(signature) +
        '}';
  }
}
