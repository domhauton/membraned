package com.domhauton.membrane.distributed.messaging.messages;

/**
 * Created by dominic on 12/02/17.
 */
public class PingMessage extends PeerMessage {
  private PingMessage() {} // For Jackson only!

  public PingMessage(String sender, String recipient, String version) {
    super(sender, recipient, -1, version);
  }

  @Override
  public void executeAction(PeerMessageActions peerMessageActions) {
    peerMessageActions.sendPongAsync(getSender(), getMessageId());
  }

  @Override
  public String toString() {
    return "PingMessage{" +
            "sender='" + sender + '\'' +
            ", recipient='" + recipient + '\'' +
            ", messageId=" + messageId +
            ", responseToMessageId=" + responseToMessageId +
            ", version='" + version + '\'' +
            '}';
  }
}
