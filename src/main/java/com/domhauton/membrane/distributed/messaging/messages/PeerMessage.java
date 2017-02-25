package com.domhauton.membrane.distributed.messaging.messages;

import com.domhauton.membrane.distributed.DistributedManager;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by dominic on 09/02/17.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="messageType")
public abstract class PeerMessage {
  private String sender;
  private String recipient;
  private String version;

  protected PeerMessage() {} // For Jackson only

  public PeerMessage(String sender, String recipient, String version) {
    this.sender = sender;
    this.recipient = recipient;
    this.version = version;
  }

  public String getSender() {
    return sender;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getVersion() {
    return version;
  }

  abstract void executeAction(DistributedManager distributedManager);

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PeerMessage that = (PeerMessage) o;

    if (sender != null ? !sender.equals(that.sender) : that.sender != null) return false;
    if (recipient != null ? !recipient.equals(that.recipient) : that.recipient != null) return false;
    return version != null ? version.equals(that.version) : that.version == null;
  }

  @Override
  public int hashCode() {
    int result = sender != null ? sender.hashCode() : 0;
    result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }
}
