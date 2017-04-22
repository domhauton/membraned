package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.auth.AuthException;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

/**
 * Created by dominic on 09/02/17.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="messageType")
public abstract class PeerMessage {
  static Logger LOGGER = LogManager.getLogger(); // Used by children
  String sender;
  String recipient;
  long messageId;
  long responseToMessageId;
  String version;

  public PeerMessage() {
    this(-1);
  }

  public PeerMessage(long responseId) {
    this.responseToMessageId = responseId;
    messageId = -1L;
  }

  public String getSender() {
    return sender;
  }

  public String getRecipient() {
    return recipient;
  }

  public long getMessageId() {
    return messageId;
  }

  public void setMessageId(long messageId) {
    this.messageId = messageId;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public long getResponseToMessageId() {
    return responseToMessageId;
  }

  public String getVersion() {
    return version;
  }

  public abstract void executeAction(PeerMessageActionProvider peerMessageActionProvider);

  public abstract void sign(RSAPrivateKey rsaPrivateKey) throws AuthException;

  public abstract void verify(X509Certificate x509Certificate) throws AuthException, PeerMessageException;

  @Override
  public String toString() {
    return "PeerMessage{" +
            "sender='" + sender + '\'' +
            ", recipient='" + recipient + '\'' +
            ", messageId=" + messageId +
            ", responseToMessageId=" + responseToMessageId +
            ", version='" + version + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PeerMessage that = (PeerMessage) o;

    if (messageId != that.messageId) return false;
    if (sender != null ? !sender.equals(that.sender) : that.sender != null) return false;
    if (recipient != null ? !recipient.equals(that.recipient) : that.recipient != null) return false;
    return version != null ? version.equals(that.version) : that.version == null;
  }

  @Override
  public int hashCode() {
    int result = sender != null ? sender.hashCode() : 0;
    result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
    result = 31 * result + (int) (messageId ^ (messageId >>> 32));
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }
}
