package com.domhauton.membrane.distributed.messaging;

import com.domhauton.membrane.distributed.messaging.messages.PongMessage;

/**
 * Created by dominic on 12/02/17.
 */
public abstract class PeerMessageUtils {
    public static PeerMessage parseMessage(byte[] message){
        return new PongMessage();
    }
}
