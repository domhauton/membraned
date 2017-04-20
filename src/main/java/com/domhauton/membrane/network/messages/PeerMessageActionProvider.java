package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.evidence.EvidenceRequest;
import com.domhauton.membrane.distributed.evidence.EvidenceResponse;
import com.domhauton.membrane.network.Gatekeeper;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messages.data.PexQueryResponseEntry;
import com.domhauton.membrane.network.messages.data.PexQueryResponseSignedEntry;
import com.domhauton.membrane.network.pex.PexEntry;
import com.domhauton.membrane.network.pex.PexManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.security.cert.X509Certificate;
import java.util.*;
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
  private final PeerCertManager peerCertManager;
  private ContractManager contractManager; // Replaceable

  PeerMessageActionProvider(ConnectionManager connectionManager, PexManager pexManager, Gatekeeper gatekeeper, PeerCertManager peerCertManager, ContractManager contractManager) {
    this.connectionManager = connectionManager;
    this.pexManager = pexManager;
    this.gatekeeper = gatekeeper;
    this.peerCertManager = peerCertManager;
    this.contractManager = contractManager;
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

  X509Certificate retrievePeerCertificate(String peerId) throws NoSuchElementException {
    return peerCertManager.getCertificate(peerId);
  }

  void processPexRequest(String targetUser, Set<String> requestedPeers, boolean requestPublic) {
    executorService.submit(() -> processPexRequestBlocking(targetUser, requestedPeers, requestPublic));
  }

  private void processPexRequestBlocking(String targetUser, Set<String> requestedPeers, boolean requestPublic) {
    // Imported to get a final version
    logger.debug("Processing PEX Request from [{}] for {} specific {}peers", targetUser, requestedPeers.size(), requestPublic ? "and public " : "");
    Set<Map.Entry<String, PexEntry>> pexEntries = pexManager.getPexEntries();
    Set<PexQueryResponseSignedEntry> pexQueryResponseSignedEntries = pexEntries.stream()
        .limit(50)
        .filter(pexEntry -> !pexEntry.getKey().equals(targetUser))
        .filter(pexEntry -> requestedPeers.contains(pexEntry.getKey()))
        .map(pexEntry -> new PexQueryResponseSignedEntry(pexEntry.getValue().getAddress(), pexEntry.getValue().getPort(), pexEntry.getKey(), pexEntry.getValue().isPublicEntry(), pexEntry.getValue().getLastUpdateDateTime(), pexEntry.getValue().getSignature()))
        .collect(Collectors.toSet());

    Set<String> ignoredPeers = new HashSet<>(requestedPeers);
    ignoredPeers.add(targetUser);
    Set<PexEntry> publicPexEntries = requestPublic ? pexManager.getPublicEntries(50, ignoredPeers) : Collections.emptySet();

    Set<PexQueryResponseEntry> unsignedResponseEntries = publicPexEntries.stream()
        .map(pexEntry -> new PexQueryResponseEntry(pexEntry.getAddress(), pexEntry.getPort()))
        .collect(Collectors.toSet());

    logger.debug("Processing PEX Request. Found {} specific peers and {} public peers. [{}].",
        pexQueryResponseSignedEntries.size(),
        unsignedResponseEntries.size(),
        targetUser);

    PexQueryResponse pexQueryResponse = new PexQueryResponse(unsignedResponseEntries, pexQueryResponseSignedEntries);

    try {
      Peer peer = connectionManager.getPeerConnection(targetUser, 15, TimeUnit.SECONDS);
      peer.sendPeerMessage(pexQueryResponse);
    } catch (TimeoutException | PeerException e) {
      logger.warn("Unable to send pex response to {}. {}", targetUser, e.getMessage());
    }
  }

  void processNewBlock(String peerId, String blockId, byte[] blockData) {
    executorService.submit(() -> processNewBlockBlocking(peerId, blockId, blockData));
  }

  private void processNewBlockBlocking(String peerId, String blockId, byte[] blockData) {
    logger.debug("Processing new block message from [{}]", peerId);
    contractManager.receiveBlock(peerId, blockId, blockData);
  }

  void processContractUpdate(String peerId, DateTime dateTime, int permittedBlockOffset, Set<String> storedBlockIds) {
    executorService.submit(() -> processContractUpdateBlocking(peerId, dateTime, permittedBlockOffset, storedBlockIds));
  }

  private void processContractUpdateBlocking(String peerId, DateTime dateTime, int permittedBlockOffset, Set<String> storedBlockIds) {
    logger.debug("Processing contract update from [{}]", peerId);
    Set<EvidenceRequest> evidenceRequests = contractManager.processPeerContractUpdate(peerId, dateTime, permittedBlockOffset, storedBlockIds);
    EvidenceRequestMessage evidenceRequestMessage = new EvidenceRequestMessage(dateTime, evidenceRequests);

    try {
      Peer peer = connectionManager.getPeerConnection(peerId, 15, TimeUnit.SECONDS);
      peer.sendPeerMessage(evidenceRequestMessage);
    } catch (TimeoutException | PeerException e) {
      logger.warn("Unable to send evidence request to {}. {}", peerId, e.getMessage());
    }
  }

  void processEvidenceRequests(String peerId, DateTime dateTime, Set<EvidenceRequest> evidenceRequests) {
    executorService.submit(() -> processEvidenceRequestsBlocking(peerId, dateTime, evidenceRequests));
  }

  private void processEvidenceRequestsBlocking(String peerId, DateTime dateTime, Set<EvidenceRequest> evidenceRequests) {
    logger.debug("Processing evidence request from [{}]", peerId);
    Set<EvidenceResponse> evidenceResponses = contractManager.processEvidenceRequests(peerId, dateTime, evidenceRequests);

    EvidenceResponseMessage evidenceResponseMessage = new EvidenceResponseMessage(dateTime, evidenceResponses);

    try {
      Peer peer = connectionManager.getPeerConnection(peerId, 15, TimeUnit.SECONDS);
      peer.sendPeerMessage(evidenceResponseMessage);
    } catch (TimeoutException | PeerException e) {
      logger.warn("Unable to send evidence request to {}. {}", peerId, e.getMessage());
    }
  }

  void processEvidenceResponse(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
    executorService.submit(() -> processEvidenceResponseBlocking(peerId, dateTime, evidenceResponses));
  }

  private void processEvidenceResponseBlocking(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
    logger.debug("Processing evidence response from [{}]", peerId);
    contractManager.processEvidenceResponses(peerId, dateTime, evidenceResponses);
  }


  void setContractManager(ContractManager contractManager) {
    this.contractManager = contractManager;
  }
}
