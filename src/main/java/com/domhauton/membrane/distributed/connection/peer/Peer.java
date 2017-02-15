package com.domhauton.membrane.distributed.connection.peer;

import com.domhauton.membrane.distributed.connection.PeerConnection;
import com.domhauton.membrane.distributed.messaging.PeerMessage;

/**
 * Created by dominic on 08/02/17.
 */
public class Peer {
    private String uid;
    private PeerConnection peerConnection;

    public Peer(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
        this.uid = peerConnection.getClientID();
    }

    public String getUid() {
        return uid;
    }

    public void sendPeerMessage(PeerMessage peerMessage) throws PeerException {
        peerConnection.sendMessage(peerMessage);
    }

    public boolean isClosed() {
        return peerConnection.isClosed();
    }

    public void close() {
        peerConnection.close();
    }
}
