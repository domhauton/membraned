package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import com.domhauton.membrane.distributed.connection.peer.Peer;
import com.domhauton.membrane.distributed.messaging.PeerMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

/**
 * Created by dominic on 08/02/17.
 *
 * Manages PeerConnection Lifecycle. Dialling, Communication and Closing.
 */
public class ConnectionManager {
    private final static Logger logger = LogManager.getLogger();

    private final PeerListener peerListener;
    private final PeerDialler peerDialler;

    private final HashMap<String, Peer> peerConnections;

    ConnectionManager(MembraneAuthInfo membraneAuthInfo, int listenPort) {
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

    /**
     * Add a new peer when connected. Ensures any previous connection is terminated.
     */
    private synchronized void addPeer(Peer peer) {
        Peer oldConnection = peerConnections.get(peer.getUid());
        if(oldConnection != null) {
            logger.info("Peer Connection - Removing connection to peer [{}]", peer.getUid());
            oldConnection.close();
        }
        logger.info("Peer Connection - New Peer Connection Established [{}]", peer.getUid());
        peerConnections.put(peer.getUid(), peer);
    }

    private void receiveMessage(PeerMessage peerMessage) {
        logger.info("Received: {}", peerMessage);
    }

    /**
     * Attempt to connect to new peer at given ip and port. Non-blocking.
     */
    void connectToPeer(String ip, int port) {
        logger.info("Dialling Peer at [{}:{}]", ip, port);
        peerDialler.dialClient(ip, port);
    }

    public int getListeningPort() {
        return peerListener.getPort();
    }

    /**
     * Close the listener and all connected peers.
     */
    public void close() {
        logger.info("Closing Connection Manager.");
        peerListener.close();
        peerConnections.values().forEach(Peer::close);
    }
}
