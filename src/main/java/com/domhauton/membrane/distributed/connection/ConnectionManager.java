package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import com.domhauton.membrane.distributed.connection.peer.Peer;
import com.domhauton.membrane.distributed.messaging.PeerMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * Created by dominic on 08/02/17.
 */
public class ConnectionManager {
    private final static Logger logger = LogManager.getLogger();

    private final MembraneAuthInfo membraneAuthInfo;
    private final PeerListener peerListener;
    private final PeerDialler peerDialler;

    private final HashMap<String, Peer> peerConnections;

    public ConnectionManager(MembraneAuthInfo membraneAuthInfo, int listenPort) {
        this.membraneAuthInfo = membraneAuthInfo;
        peerConnections = new HashMap<>();
        this.peerListener = new PeerListener(
                listenPort,
                this::addPeer,
                this::receiveMessage,
                membraneAuthInfo);
        peerListener.start();
        this.peerDialler = new PeerDialler(
                this::addPeer,
                this::receiveMessage,
                membraneAuthInfo);
    }

    private synchronized void addPeer(Peer peer) {
        Peer oldConnection = peerConnections.get(peer.getUid());
        if(oldConnection != null) {
            oldConnection.close();
        }
        peerConnections.put(peer.getUid(), peer);
    }

    private void receiveMessage(PeerMessage peerMessage) {
        logger.info("Received: {}", peerMessage);
    }

    public void connectToPeer(String ip, int port) {
        logger.info("Dialling Peer at [{}:{}]", ip, port);
        peerDialler.dialClient(ip, port);
    }

    public int getListeningPort() {
        return peerListener.getPort();
    }

    public void close() {
        logger.info("Closing Connection Manager.");
        peerListener.close();
        peerConnections.values().forEach(Peer::close);
    }
}
