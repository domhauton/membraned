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
import org.joda.time.Minutes;

import java.util.HashSet;
import java.util.Iterator;
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
  private static final int PEER_MAINTENANCE_FREQUENCY_SEC = 120;

  private final Logger logger = LogManager.getLogger();
  private final ConnectionManager connectionManager;
  private final PortForwardingService portForwardingService;
  private final PeerCertManager peerCertManager;
  private ContractManager contractManager; // Should be replaceable.


  private final PexManager pexManager;
  private final Set<String> friendPeers;
  private final Set<Tracker> trackers;

  private final ScheduledExecutorService peerMaintainerExecutor;

  private int maxConnections;
  private boolean searchForNewPublicPeers = false;
  private final DateTime startUpDateTime;

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
    startUpDateTime = DateTime.now();
  }

  void maintainPeerPopulation() {
    // First attempt to connect to any peers with pre-established contracts
    connectToContractedPeers();

    // Check if new connections are required and have space

    int remainingConnectionCount = remainingConnections();
    boolean requireMoreConnections = requiredConnections() > 0;
    boolean haveRemainingConnections = remainingConnectionCount > 0;

    if (requireMoreConnections && haveRemainingConnections) {
      requestPexInformation();
      if (searchForNewPublicPeers) {
        connectToPublicPeersInPex();
      }
    }

    sendPexUpdate(searchForNewPublicPeers && requireMoreConnections);

    // Disconnect from peers taking up useful connection slots

    if (requireMoreConnections && !haveRemainingConnections) {
      disconnectRedundantPeers(-remainingConnectionCount);
    }

    // Connect to a tracker if struggling for peers.

    if (shouldConnectToTrackers()) {
      connectToTrackers();
    }
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
    if (searchForNewPublicPeers && !isConnected.get() && newPeersRequired.get() && remainingConnections.get()) {
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
    relatedPeers.removeAll(trackers.stream().map(Tracker::getPeerId).collect(Collectors.toSet()));
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
    totalPeers.removeAll(trackers.stream().map(Tracker::getPeerId).collect(Collectors.toSet()));
    return totalPeers.size() - maxConnections;
  }

  /**
   * Initiates connections to contracted, unconnected peers with pex info.
   */
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

  /**
   * Send updated PEX information to all peers.
   *
   * @param isPublic
   */
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

  /**
   * Requests all connected peers for PEX information
   */
  private void requestPexInformation() {
    Set<String> lostPeers = new HashSet<>();
    lostPeers.addAll(contractManager.getContractedPeers());
    lostPeers.removeAll(connectionManager.getAllConnectedPeers().stream().map(Peer::getUid).collect(Collectors.toSet()));

    if (!lostPeers.isEmpty()) {
      PexQueryRequest pexQueryRequest = new PexQueryRequest(contractManager.getContractedPeers(), searchForNewPublicPeers);
      for (Peer peer : connectionManager.getAllConnectedPeers()) {
        try {
          peer.sendPeerMessage(pexQueryRequest);
        } catch (PeerException e) {
          logger.warn("Failed to send PEX request to [{}]. {}", peer.getUid(), e.getMessage());
        }
      }
    }
  }

  /**
   * Should a tracker connection be initiated based on startup time, and expected peer ratios.
   *
   * @return True if connection to trackers is needed
   */
  private boolean shouldConnectToTrackers() {
    int contractedPeerTarget = contractManager.getContractCountTarget();
    int minutesFromStartup = Minutes.minutesBetween(startUpDateTime, DateTime.now()).getMinutes();
    if (contractManager == null || contractedPeerTarget < 0 || minutesFromStartup < 5) {
      return false;
    }
    int connectedPeerCount = maxConnections - remainingConnections();
    float connectedPeerRatio = (float) connectedPeerCount / (float) contractedPeerTarget;
    float expectedPeerRatio = (float) minutesFromStartup / (float) 200;
    return connectedPeerRatio < expectedPeerRatio;
  }

  /**
   * Initiates a connection to every tracker on the tracker list.
   */
  private void connectToTrackers() {
    trackers.forEach(x -> connectionManager.connectToPeer(x.getIp(), x.getPort()));
  }

  /**
   * Selects redundant peers and disconnects from them.
   */
  private void disconnectRedundantPeers(int peersToDisconnect) {
    logger.info("Disconnecting {} redundant peers.", peersToDisconnect);
    Set<String> contractedPeers = contractManager.getContractedPeers();
    Set<String> trackerPeerIds = trackers.stream().map(Tracker::getPeerId).collect(Collectors.toSet());

    connectionManager.getAllConnectedPeerIds().stream()
        .filter(x -> !contractedPeers.contains(x))
        .filter(x -> !trackerPeerIds.contains(x))
        .limit(peersToDisconnect)
        .forEach(this::disconnectPeer);
  }

  /**
   * Disconnect from the given peer.
   * @param peerId  Peer to disconnect from
   */
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

  /**
   * Attempt connections to provided public PEX entries.
   */
  private void connectToPublicPeersInPex() {
    pexManager.getPublicEntries(requiredConnections())
        .forEach(x -> connectionManager.connectToPeer(x.getAddress(), x.getPort()));
  }


  public void setContractManager(ContractManager contractManager) {
    this.contractManager = contractManager;
    this.peerCertManager.setContractManager(contractManager);
    connectToPublicPeersInPex();
  }

  public void setSearchForNewPublicPeers(boolean searchForNewPublicPeers) {
    this.searchForNewPublicPeers = searchForNewPublicPeers;
  }

  @Override
  public void run() {
    peerMaintainerExecutor.scheduleWithFixedDelay(this::maintainPeerPopulation, 0, PEER_MAINTENANCE_FREQUENCY_SEC, TimeUnit.SECONDS);
  }

  public void close() {
    peerMaintainerExecutor.shutdown();
  }
}
