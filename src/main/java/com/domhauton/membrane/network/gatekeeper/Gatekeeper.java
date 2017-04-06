package com.domhauton.membrane.network.gatekeeper;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.DistributorException;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messages.PexAdvertisement;
import com.domhauton.membrane.network.messages.PexQueryRequest;
import com.domhauton.membrane.network.pex.PexEntry;
import com.domhauton.membrane.network.pex.PexException;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.upnp.ExternalAddress;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.DateTimeConstants;
import org.joda.time.Hours;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by dominic on 29/03/17.
 */
public class Gatekeeper implements Runnable {
  private static final int HOURS_BEFORE_PEX_ENTRY_CONSIDERED_OLD = DateTimeConstants.HOURS_PER_WEEK;
  private static final int PEER_MAINTENANCE_FREQUENCY_SEC = 120;

  private final Logger logger = LogManager.getLogger();
  private final ConnectionManager connectionManager;
  private final PortForwardingService portForwardingService;
  private final PeerCertManager peerCertManager;
  private ContractManager contractManager; // Should be replaceable.


  private final PexManager pexManager;
  private final Set<String> friendPeers;
  private final Set<String> trackers;

  private final ScheduledExecutorService peerMaintainerExecutor;

  private int maxConnections;
  private boolean isSearchingForNewPeers = false;

  public Gatekeeper(ConnectionManager connectionManager, ContractManager contractManager, PexManager pexManager, PortForwardingService portForwardingService, PeerCertManager peerCertManager, int maxConnections) {
    this.connectionManager = connectionManager;
    this.contractManager = contractManager;
    this.pexManager = pexManager;
    this.maxConnections = maxConnections;
    this.portForwardingService = portForwardingService;
    this.peerCertManager = peerCertManager;

    this.friendPeers = new HashSet<>();
    this.trackers = new HashSet<>();

    peerMaintainerExecutor = Executors.newSingleThreadScheduledExecutor();
  }

  void maintainPeerPopulation() {
    connectToContractedPeers();
    connectToPeersInPex();

    int requiredConnections = requiredConnections();
    int remainingConnections = remainingConnections();

    boolean sendPublicPexUpdate = false;

    if (requiredConnections > 0 && remainingConnections > 0) {
      requestPexInformation();
      if (isSearchingForNewPeers) {
        sendPublicPexUpdate = true;
      }
    } else if (remainingConnections < 0) {
      disconnectRedundantPeers(-remainingConnections);
    }

    sendPexUpdate(sendPublicPexUpdate);
  }

  public void processNewPeerConnected(Peer peer) {
    Supplier<Boolean> isContracted = () -> contractManager.getContractedPeers().contains(peer.getUid());
    Supplier<Boolean> newPeersRequired = () -> requiredConnections() > 0;

    if (!isContracted.get() && newPeersRequired.get()) {
      logger.info("New peer discovered. Assigning to contract.");
      try {
        contractManager.addContractedPeer(peer.getUid());
        peerCertManager.addCertificate(peer.getUid(), peer.getX509Certificate());
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
  public void processNewPexEntry(String peerId, PexEntry pexEntry) {
    // Suppliers for lazy loading / short circuit
    Supplier<Boolean> isConnected = () -> connectionManager.getAllConnectedPeerIds().contains(peerId);
    Supplier<Boolean> newPeersRequired = () -> requiredConnections() > 0;
    Supplier<Boolean> remainingConnections = () -> remainingConnections() > 0;

    // Try to dial peer if all conditions satisfied.
    if (isSearchingForNewPeers && !isConnected.get() && newPeersRequired.get() && remainingConnections.get()) {
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

  private void sendPexUpdate(boolean isPublic) {
    Iterator<ExternalAddress> externalAddresses = portForwardingService.getExternallyMappedAddresses().iterator();
    ExternalAddress externalAddress = externalAddresses.hasNext() ?
        externalAddresses.next() : portForwardingService.getNonForwardedAddress();
    PexAdvertisement pexAdvertisement = new PexAdvertisement(externalAddress.getIpAddress(), externalAddress.getPort(), isPublic, DateTime.now());

    for (Peer peer : connectionManager.getAllConnectedPeers()) {
      try {
        peer.sendPeerMessage(pexAdvertisement);
      } catch (PeerException e) {
        logger.warn("Failed to send PEX request to [{}]. {}", peer.getUid(), e.getMessage());
      }
    }
  }

  private void requestPexInformation() {
    Set<String> lostPeers = new HashSet<>();
    lostPeers.addAll(contractManager.getContractedPeers());
    lostPeers.removeAll(connectionManager.getAllConnectedPeers().stream().map(Peer::getUid).collect(Collectors.toSet()));

    if (!lostPeers.isEmpty()) {
      PexQueryRequest pexQueryRequest = new PexQueryRequest(contractManager.getContractedPeers(), isSearchingForNewPeers);
      for (Peer peer : connectionManager.getAllConnectedPeers()) {
        try {
          peer.sendPeerMessage(pexQueryRequest);
        } catch (PeerException e) {
          logger.warn("Failed to send PEX request to [{}]. {}", peer.getUid(), e.getMessage());
        }
      }
    }
  }

  void disconnectRedundantPeers(int peersToDisconnect) {
    logger.info("Disconnecting {} redundant peers.", peersToDisconnect);
    Set<String> contractedPeers = contractManager.getContractedPeers();

    connectionManager.getAllConnectedPeerIds().stream()
        .filter(x -> !contractedPeers.contains(x))
        .filter(x -> !trackers.contains(x))
        .limit(peersToDisconnect)
        .forEach(this::disconnectPeer);
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
    this.peerCertManager.setContractManager(contractManager);
    connectToPeersInPex();
  }

  public void setSearchingForNewPeers(boolean searchingForNewPeers) {
    isSearchingForNewPeers = searchingForNewPeers;
  }

  @Override
  public void run() {
    peerMaintainerExecutor.scheduleWithFixedDelay(this::maintainPeerPopulation, 0, PEER_MAINTENANCE_FREQUENCY_SEC, TimeUnit.SECONDS);
  }

  public void close() {
    peerMaintainerExecutor.shutdown();
  }
}
