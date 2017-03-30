package com.domhauton.membrane.network.gatekeeper;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.DistributorException;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messaging.messages.PeerMessage;
import com.domhauton.membrane.network.pex.PexEntry;
import com.domhauton.membrane.network.pex.PexException;
import com.domhauton.membrane.network.pex.PexManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.DateTimeConstants;
import org.joda.time.Hours;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by dominic on 29/03/17.
 */
public class Gatekeeper {
  private static final int HOURS_BEFORE_PEX_ENTRY_CONSIDERED_OLD = DateTimeConstants.HOURS_PER_WEEK;

  private final Logger logger = LogManager.getLogger();
  private final ConnectionManager connectionManager;

  private final PexManager pexManager;
  private final Set<String> friendPeers;
  private final Set<String> trackers;

  private int maxConnections;
  private boolean isSearchingForNewPeers;

  private ContractManager contractManager;

  public Gatekeeper(ConnectionManager connectionManager, ContractManager contractManager, PexManager pexManager, int maxConnections) {
    this.connectionManager = connectionManager;
    this.contractManager = contractManager;
    this.pexManager = pexManager;
    this.maxConnections = maxConnections;

    this.friendPeers = new HashSet<>();
    this.trackers = new HashSet<>();
  }

  private void newPeerConnected(String peerId) {
    //TODO If is searching for new peers, needs new peers and this is a new peer, send PEX and add to contracts
    Supplier<Boolean> isContracted = () -> contractManager.getContractedPeers().contains(peerId);
    Supplier<Boolean> newPeersRequired = () -> requiredConnections() > 0;

    if (!isContracted.get() && newPeersRequired.get()) {
      logger.info("New peer discovered. Assigning to contract.");
      try {
        contractManager.addContractedPeer(peerId);
      } catch (DistributorException e) {
        logger.warn("Unable to contract new peer.");
      }
    }
  }

  /**
   * Inform gatekeeper of a new PEX entry. If user matches all criteria attempt to connect
   *
   * @param peerId   User pex entry is about.
   * @param pexEntry PEX entry information.
   */
  private void recievePEX(String peerId, PexEntry pexEntry) {
    // Suppliers for lazy loading / short circuit
    Supplier<Boolean> isContracted = () -> contractManager.getContractedPeers().contains(peerId);
    Supplier<Boolean> isConnected = () -> connectionManager.getAllConnectedPeerIds().contains(peerId);
    Supplier<Boolean> newPeersRequired = () -> requiredConnections() > 0;
    Supplier<Boolean> remainingConnections = () -> remainingConnections() > 0;

    // Try to dial peer if all conditions satisfied.
    if (isSearchingForNewPeers && !isConnected.get() && !isContracted.get() && newPeersRequired.get() && remainingConnections.get()) {
      connectionManager.connectToPeer(pexEntry.getAddress(), pexEntry.getPort());
    }
  }

  /**
   * Total number of connections required to reach target for contract.
   *
   * @return number of required connections. Can be negative.
   */
  private int requiredConnections() {
    // Find all peers known and unconnected.
    Set<String> relatedPeers = new HashSet<>();
    relatedPeers.addAll(connectionManager.getAllConnectedPeerIds());
    relatedPeers.addAll(contractManager.getContractedPeers());
    relatedPeers.addAll(friendPeers);
    relatedPeers.removeAll(trackers);
    return contractManager.getContractCountTarget() - relatedPeers.size();
  }

  /**
   * Total number of connections remaining.
   *
   * @return number of required connections. Can be negative.
   */
  private int remainingConnections() {
    Set<String> totalPeers = new HashSet<>();
    totalPeers.addAll(connectionManager.getAllConnectedPeerIds());
    totalPeers.addAll(contractManager.getContractedPeers());
    totalPeers.addAll(friendPeers);
    totalPeers.removeAll(trackers);
    return totalPeers.size() - maxConnections;
  }

  private void connectToContractedPeers() {
    Set<String> lostPeers = new HashSet<>();
    lostPeers.addAll(contractManager.getContractedPeers());
    lostPeers.removeAll(connectionManager.getAllConnectedPeerIds());

    // Try to reconnect to everyone based on missing pex information.
    for (String peer : lostPeers) {
      try {
        PexEntry entry = pexManager.getEntry(peer);
        connectionManager.connectToPeer(entry.getAddress(), entry.getPort());
      } catch (PexException e) {
        e.printStackTrace();
      }
    }
  }

  private void requestPexInformation() {
    Set<String> lostPeers = new HashSet<>();
    lostPeers.addAll(contractManager.getContractedPeers());
    lostPeers.removeAll(connectionManager.getAllConnectedPeers().stream().map(Peer::getUid).collect(Collectors.toSet()));

    if (!lostPeers.isEmpty()) {
      PeerMessage peerMessage = null; //FIXME generate PEX info request.
      for (Peer peer : connectionManager.getAllConnectedPeers()) {
        try {
          peer.sendPeerMessage(peerMessage);
        } catch (PeerException e) {
          logger.warn("Failed to send PEX request to [{}]. {}", peer.getUid(), e.getMessage());
        }
      }
    }
  }

  private void disconnectPeer(String peerId) {
    logger.info("Starting peer disconnect: [{}]", peerId);
    try {
      Peer peer = connectionManager.getPeerConnection(peerId, 0, TimeUnit.SECONDS);
      peer.close(); // Async. Won't happen immediately.
    } catch (TimeoutException e) {
      // Disconnected anyway doesn't matter.
      logger.info("Peer not found for disconnect. Ignoring. [{}]", peerId);
    }
  }

  private void connectToPeersInPex() {
    Set<String> connectedPeers = connectionManager.getAllConnectedPeerIds();
    // Start a connection request for every peer in pex young enough for an entry
    pexManager.getPexEntries().stream()
        .filter(x -> !connectedPeers.contains(x.getKey()))
        .map(Map.Entry::getValue)
        .filter(x -> Hours.hoursBetween(x.getLastUpdateDateTime(), DateTime.now()).isLessThan(Hours.hours(HOURS_BEFORE_PEX_ENTRY_CONSIDERED_OLD)))
        .sorted((x1, x2) -> DateTimeComparator.getInstance().compare(x1.getLastUpdateDateTime(), x2.getLastUpdateDateTime()))
        .limit(requiredConnections())
        .forEach(x -> connectionManager.connectToPeer(x.getAddress(), x.getPort()));
  }


  public void setContractManager(ContractManager contractManager) {
    this.contractManager = contractManager;
  }
}
