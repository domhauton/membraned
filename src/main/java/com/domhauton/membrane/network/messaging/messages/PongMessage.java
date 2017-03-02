package com.domhauton.membrane.network.messaging.messages;

/**
 * Created by dominic on 12/02/17.
 */
public class PongMessage extends PeerMessage {

  private PongMessage() {} // For Jackson only!

  public PongMessage(String sender, String recipient, String version, long pingID) {
    super(sender, recipient, pingID, version);
  }

  @Override
  public void executeAction(PeerMessageActions peerMessageActions) {

  }

  @Override
  public String toString() {
    return "PongMessage{" +
            "sender='" + sender + '\'' +
            ", recipient='" + recipient + '\'' +
            ", messageId=" + messageId +
            ", responseToMessageId=" + responseToMessageId +
            ", version='" + version + '\'' +
            '}';
  }
}
