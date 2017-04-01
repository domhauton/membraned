package com.domhauton.membrane.network.messaging.messages;

import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by Dominic Hauton on 26/02/17.
 */
public class PeerMessageActions {
  private final static Logger logger = LogManager.getLogger();
  private final ConnectionManager connectionManager;
  private final String userID;
  private final ExecutorService executorService;

  public PeerMessageActions(ConnectionManager connectionManager, String userID) {
    this.connectionManager = connectionManager;
    this.userID = userID;
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("memb-peer-msg-pool-%d")
            .build();
    executorService = Executors.newCachedThreadPool(threadFactory);
  }

  void sendPongAsync(String targetUser, long pingId) {
    executorService.submit(() -> sendPong(targetUser, pingId));
  }

  private void sendPong(String targetUser, long pingId) {
    try {
      Peer peer = connectionManager.getPeerConnection(targetUser, 15, TimeUnit.SECONDS);
      peer.sendPeerMessage(new PongMessage(pingId));
    } catch (TimeoutException | PeerException e) {
      logger.warn("Unable to send pong to {}. {}", targetUser, e.getMessage());
    }
  }

  void processSignedPexInfo(String peer, String ip, int port, boolean isPublic, DateTime dateTime, byte[] signature) {
    //FIXME Implement
  }

  void processUnsignedPexInfo(String ip, int port) {
    //FIXME Implement
  }

  X509Certificate retrievePeerCertificate(String peerId) throws AuthException {
    //FIXME Actually Retrieve cert;
    return null;
  }

  void processPexRequest(Set<String> requestedPeers, boolean requestPublic) {
    //FIXME Implement
  }
}
