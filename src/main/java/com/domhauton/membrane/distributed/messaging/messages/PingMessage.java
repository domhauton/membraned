package com.domhauton.membrane.distributed.messaging.messages;

import com.domhauton.membrane.distributed.messaging.PeerMessageActions;

/**
 * Created by dominic on 12/02/17.
 */
public class PingMessage extends PeerMessage {
  private PingMessage() {} // For Jackson only!

  public PingMessage(String sender, String recipient, String version) {
    super(sender, recipient, version);
  }

  @Override
  void executeAction(PeerMessageActions peerMessageActions) {

  }
}
