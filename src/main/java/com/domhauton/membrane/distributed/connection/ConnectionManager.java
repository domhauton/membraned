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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
  private final Lock peerConnectionLock = new ReentrantLock();
  private final Condition peerConnectionChangeOccurred = peerConnectionLock.newCondition();

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
    peerConnectionLock.lock();
    Peer oldConnection = peerConnections.get(peer.getUid());
    if (oldConnection != null) {
      logger.info("Peer Connection - Removing connection to peer [{}]", peer.getUid());
      oldConnection.close();
    }
    logger.info("Peer Connection - New Peer Connection Established [{}]", peer.getUid());
    peerConnections.put(peer.getUid(), peer);
    peerConnectionChangeOccurred.signalAll();
    peerConnectionLock.unlock();
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

  /**
   * Return peer within timeout or error.
   *
   * @param peerId   The id of the peer to retrieve
   * @param timeout  Time to wait in @timeUnit
   * @param timeUnit Unit of time to wait
   * @return A peer. Best effort actually connected peer.
   * @throws TimeoutException If the peer could not be found in the given time.
   */
  public Peer getPeerConnection(String peerId, long timeout, TimeUnit timeUnit) throws TimeoutException {
    logger.trace("Retrieving peer connection {}. Timeout: {} {}", peerId, timeout, timeUnit);
    try {
      Peer peer = CompletableFuture.supplyAsync(() -> getPeerConnection(peerId)).get(timeout, timeUnit);
      if (peer != null) {
        return peer;
      } else {
        throw new InterruptedException("Interrupted before correct peer retrieved");
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      logger.warn("Could not find peer {} in {} {}. Cancelling.", peerId, timeout, timeUnit);
      throw new TimeoutException("Unable to find peer " + peerId);
    }

  }

  /**
   * Return peer or block for new connections if not already
   *
   * @return returns peer unless interrupted. Returns null otherwise
   */
  private Peer getPeerConnection(String peerId) {
    peerConnectionLock.lock();
    Peer peer = peerConnections.getOrDefault(peerId, null);

    // Clear up a closed connection.

    if (peer != null && peer.isClosed()) {
      peerConnections.remove(peerId);
      peer = null;
    }

    // Wait until a peer turns up. Check if it's the peer we are waiting for.

    try {
      while (peer == null) {
        // Unlock and wait for a change in peers.

        peerConnectionChangeOccurred.await();
        peer = peerConnections.getOrDefault(peerId, null);

        // Clear up a closed connection.

        if (peer != null && peer.isClosed()) {
          peerConnections.remove(peerId);
          peer = null;
        }
      }
    } catch (InterruptedException e) {
      logger.warn("Interrupted while waiting for peer [{}]", peerId);
    } finally {
      peerConnectionLock.unlock();
    }
    return peer;
  }

  synchronized void registerNewPeerCallback(Consumer<Peer> consumer) {
    newPeerJoinedCallbacks.add(consumer);
  }

  synchronized void registerMessageCallback(Consumer<PeerMessage> peerMessageConsumer) {
    newPeerMessageCallbacks.add(peerMessageConsumer);
  }

  Collection<Peer> getAllConnectedPeers() {
    return peerConnections.values();
  }

  /**
   * Close the listener and all connected peers.
   */
  public void close() {
    logger.info("Closing Connection Manager.");
    peerListener.close();
    peerConnectionLock.lock();
    peerConnections.values().forEach(Peer::close);
    peerConnectionChangeOccurred.signalAll();
    peerConnectionLock.unlock();
  }


}
