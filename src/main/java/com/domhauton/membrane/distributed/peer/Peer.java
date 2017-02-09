package com.domhauton.membrane.distributed.peer;

import com.domhauton.membrane.distributed.peer.connection.PeerConnection;

/**
 * Created by dominic on 08/02/17.
 */
public class Peer {
    private String uid;
    private PeerConnection peerConnection;

    private Peer(PeerConnection peerConnection) {
        this.peerConnection.getIP();
    }
}
