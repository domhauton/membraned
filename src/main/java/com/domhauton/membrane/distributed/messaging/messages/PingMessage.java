package com.domhauton.membrane.distributed.messaging.messages;

import com.domhauton.membrane.distributed.messaging.PeerMessage;

/**
 * Created by dominic on 12/02/17.
 */
public class PingMessage implements PeerMessage{
    @Override
    public byte[] getBytes() {
        return "PING".getBytes();
    }
}
