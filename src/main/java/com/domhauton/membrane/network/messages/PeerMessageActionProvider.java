package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.gatekeeper.Gatekeeper;
import com.domhauton.membrane.network.messages.data.PexQueryResponseEntry;
import com.domhauton.membrane.network.messages.data.PexQueryResponseSignedEntry;
import com.domhauton.membrane.network.pex.PexEntry;
import com.domhauton.membrane.network.pex.PexManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 26/02/17.
 */
class PeerMessageActionProvider {
  private final Logger logger = LogManager.getLogger();

  private final ExecutorService executorService;

  private final ConnectionManager connectionManager;
  private final PexManager pexManager;
  private final Gatekeeper gatekeeper;

  PeerMessageActionProvider(ConnectionManager connectionManager, PexManager pexManager, Gatekeeper gatekeeper) {
    this.connectionManager = connectionManager;
    this.pexManager = pexManager;
    this.gatekeeper = gatekeeper;
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
    Optional<PexEntry> pexEntryOptional = pexManager.addEntry(peer, ip, port, isPublic, dateTime, signature);
    pexEntryOptional.ifPresent(pexEntry -> gatekeeper.processNewPexEntry(peer, pexEntry));
  }

  void processUnsignedPexInfo(String ip, int port) {
    pexManager.addUnconfirmedEntry(ip, port);
  }

  X509Certificate retrievePeerCertificate(String peerId) throws AuthException {
    //FIXME Actually Retrieve cert;
    return null;
  }

  void processPexRequest(String targetUser, Set<String> requestedPeers, boolean requestPublic) {
    executorService.submit(() -> processPexRequestBlocking(targetUser, requestedPeers, requestPublic));
  }

  private void processPexRequestBlocking(String targetUser, Set<String> requestedPeers, boolean requestPublic) {
    // Imported to get a final version
    Set<Map.Entry<String, PexEntry>> pexEntries = pexManager.getPexEntries();
    Set<PexQueryResponseSignedEntry> pexQueryResponseSignedEntries = pexEntries.stream()
        .limit(50)
        .filter(pexEntry -> requestedPeers.contains(pexEntry.getKey()))
        .map(pexEntry -> new PexQueryResponseSignedEntry(pexEntry.getValue().getAddress(), pexEntry.getValue().getPort(), pexEntry.getKey(), pexEntry.getValue().isPublicEntry(), pexEntry.getValue().getLastUpdateDateTime(), pexEntry.getValue().getSignature()))
        .collect(Collectors.toSet());

    Set<PexEntry> publicPexEntries = requestPublic ? pexManager.getPublicEntries(50) : Collections.emptySet();

    Set<PexQueryResponseEntry> unsignedResponseEntries = publicPexEntries.stream()
        .map(pexEntry -> new PexQueryResponseEntry(pexEntry.getAddress(), pexEntry.getPort()))
        .collect(Collectors.toSet());

    PexQueryResponse pexQueryResponse = new PexQueryResponse(unsignedResponseEntries, pexQueryResponseSignedEntries);

    try {
      Peer peer = connectionManager.getPeerConnection(targetUser, 15, TimeUnit.SECONDS);
      peer.sendPeerMessage(pexQueryResponse);
    } catch (TimeoutException | PeerException e) {
      logger.warn("Unable to send pex response to {}. {}", targetUser, e.getMessage());
    }
  }
}
