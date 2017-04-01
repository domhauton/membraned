package com.domhauton.membrane.network.connection;

import com.domhauton.membrane.network.messaging.messages.PingMessage;

/**
 * Created by dominic on 01/04/17.
 */
class MasqueradePingMessage extends PingMessage {
  @Override
  public void setSender(String sender) {
    super.setSender("corrruption-" + sender);
  }
}
