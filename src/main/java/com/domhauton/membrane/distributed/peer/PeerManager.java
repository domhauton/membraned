package com.domhauton.membrane.distributed.peer;

import com.domhauton.membrane.distributed.peer.connection.PeerConnection;
import com.domhauton.membrane.distributed.peer.connection.PeerDialler;
import com.domhauton.membrane.distributed.peer.connection.PeerListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dominic on 08/02/17.
 */
public class PeerManager {
    private final PeerDialler peerDialler;
    private final PeerListener peerListener;
    private final Map<String, Peer> peerMap;

    public PeerManager(int listenerPort) {
        peerMap = new HashMap<>();
        peerDialler = new PeerDialler();
        peerListener = new PeerListener();
    }

    public Peer getPeer(String PID){

    }
}
