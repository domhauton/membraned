package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import com.domhauton.membrane.distributed.connection.peer.Peer;
import com.domhauton.membrane.distributed.messaging.messages.PeerMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Created by dominic on 08/02/17.
 * <p>
 * Manages PeerConnection Lifecycle. Dialling, Communication and Closing.
 */
public class ConnectionManager implements Closeable {
  private final static Logger logger = LogManager.getLogger();

  private final PeerListener peerListener;
  private final PeerDialler peerDialler;

  private final ConcurrentHashMap<String, Peer> peerConnections;

  private final Collection<Consumer<Peer>> newPeerJoinedCallbacks;
  private final Collection<Consumer<PeerMessage>> newPeerMessageCallbacks;

  public ConnectionManager(MembraneAuthInfo membraneAuthInfo, int listenPort) throws ConnectionException {
    peerConnections = new ConcurrentHashMap<>();
    newPeerJoinedCallbacks = new LinkedList<>();
    newPeerMessageCallbacks = new LinkedList<>();
    this.peerListener = new PeerListener(
            listenPort,
            this::addPeer,
            this::receiveMessage,
            membraneAuthInfo);
    CompletableFuture<Boolean> successfullyConnectedFuture = new CompletableFuture<>();
    peerListener.start(successfullyConnectedFuture);
    this.peerDialler = new PeerDialler(
            this::addPeer,
            this::receiveMessage,
            membraneAuthInfo);
    try {
      if (!successfullyConnectedFuture.get(5, TimeUnit.SECONDS)) {
        throw new ConnectionException("Server could not start listening successfully.");
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      logger.warn("Server could not start listening in time. {}", e.getMessage());
      throw new ConnectionException("Server could not start listening in time. " + e.getMessage());
    }
  }

  /**
   * Add a new peer when connected. Ensures any previous connection is terminated.
   */
  private synchronized void addPeer(Peer peer) {
    Peer oldConnection = peerConnections.get(peer.getUid());
    if (oldConnection != null) {
      logger.info("Peer Connection - Removing connection to peer [{}]", peer.getUid());
      oldConnection.close();
    }
    logger.info("Peer Connection - New Peer Connection Established [{}]", peer.getUid());
    peerConnections.put(peer.getUid(), peer);
    newPeerJoinedCallbacks.forEach(x -> x.accept(peer));
  }

  private void receiveMessage(PeerMessage peerMessage) {
    logger.info("Received: {}", peerMessage);
    newPeerMessageCallbacks.forEach(x -> x.accept(peerMessage));
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

  public synchronized void registerNewPeerCallback(Consumer<Peer> consumer) {
    newPeerJoinedCallbacks.add(consumer);
  }

  public synchronized void registerMessageCallback(Consumer<PeerMessage> peerMessageConsumer) {
    newPeerMessageCallbacks.add(peerMessageConsumer);
  }

  public Collection<Peer> getAllConnectedPeers() {
    return peerConnections.values();
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
