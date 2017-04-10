package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.DistributorException;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.pex.PexEntry;
import com.domhauton.membrane.network.pex.PexException;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.tracker.TrackerManager;
import com.domhauton.membrane.network.upnp.ExternalAddress;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by dominic on 29/03/17.
 */
public class Gatekeeper implements Runnable {
  private static final int PEER_MAINTENANCE_FREQUENCY_SEC = 120;

  private final Logger logger = LogManager.getLogger();
  private final ConnectionManager connectionManager;
  private final PortForwardingService portForwardingService;
  private final PeerCertManager peerCertManager;
  private final TrackerManager trackerManager;
  private ContractManager contractManager; // Should be replaceable.


  private final PexManager pexManager;
  private final Set<String> friendPeers;

  private final ScheduledExecutorService peerMaintainerExecutor;

  private int maxConnections;
  private boolean searchForNewPublicPeers = false;
  private final DateTime startUpDateTime;

  public Gatekeeper(ConnectionManager connectionManager, ContractManager contractManager, PexManager pexManager, PortForwardingService portForwardingService, PeerCertManager peerCertManager, TrackerManager trackerManager, int maxConnections) {
    this.connectionManager = connectionManager;
    this.contractManager = contractManager;
    this.pexManager = pexManager;
    this.maxConnections = maxConnections;
    this.portForwardingService = portForwardingService;
    this.peerCertManager = peerCertManager;
    this.trackerManager = trackerManager;

    this.friendPeers = new HashSet<>();

    peerMaintainerExecutor = Executors.newSingleThreadScheduledExecutor();
    startUpDateTime = DateTime.now();
  }

  private void maintainPeerPopulation() {
    Set<String> contractedPeers = contractManager.getContractedPeers();
    Set<String> connectedPeers = connectionManager.getAllConnectedPeerIds();
    Set<String> trackerPeers = trackerManager.getTrackerIds();

    // First attempt to connect to any peers with pre-established contracts
    connectToContractedPeers(contractedPeers, connectedPeers);


    // Check if new connections are required and have space
    int knownPeerCount = getKnownPeerCount(contractedPeers, connectedPeers, friendPeers, trackerPeers);
    int remainingConnectionCount = remainingConnections(knownPeerCount, maxConnections);
    int requiredConnections = requiredPeers(knownPeerCount, contractManager.getContractCountTarget());

    boolean requireMoreConnections = requiredConnections > 0;
    boolean haveRemainingConnections = remainingConnectionCount > 0;

    Collection<Peer> connectedPeerSet = connectionManager.getAllConnectedPeers();

    if (requireMoreConnections && haveRemainingConnections) {
      PexManager.requestPexInformation(connectedPeerSet, contractedPeers, searchForNewPublicPeers);
      if (searchForNewPublicPeers) {
        pexManager.connectToPublicPeersInPex(connectionManager, requiredConnections);
      }
    }

    boolean isPexUpdatePublic = searchForNewPublicPeers && requireMoreConnections;

    ExternalAddress externalAddress = portForwardingService.getExternalAddress();
    PexManager.sendPexUpdate(externalAddress, connectedPeerSet, isPexUpdatePublic);

    // Disconnect from peers taking up useful connection slots

    if (requireMoreConnections && !haveRemainingConnections) {
      disconnectRedundantPeers(-remainingConnectionCount);
    }

    // Connect to a tracker if struggling for peers.

    int contractedPeerTarget = contractManager == null ? 0 : contractManager.getContractCountTarget();
    int connectedPeerCount = maxConnections - remainingConnectionCount;
    int minutesFromStartup = Minutes.minutesBetween(startUpDateTime, DateTime.now()).getMinutes();

    if (trackerManager.shouldConnectToTrackers(contractedPeerTarget, minutesFromStartup, connectedPeerCount)) {
      trackerManager.connectToTrackers(connectionManager);
    }
  }

  public void processNewPeerConnected(Peer peer) {
    boolean isContracted = contractManager.getContractedPeers().contains(peer.getUid());
    int contractTarget = contractManager.getContractCountTarget();

    // If is contracted or need new peers
    if (isContracted || requiredPeers(getKnownPeerCount(), contractTarget) > 0) {
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
    boolean peerConnected = connectionManager.getAllConnectedPeerIds().contains(peerId);
    int knownPeers = getKnownPeerCount();
    boolean newPeersRequired = requiredPeers(knownPeers, contractManager.getContractCountTarget()) > 0;
    boolean remainingConnections = remainingConnections(knownPeers, maxConnections) > 0;

    // Try to dial peer if all conditions satisfied.
    if (searchForNewPublicPeers && !peerConnected && newPeersRequired && remainingConnections) {
      connectionManager.connectToPeer(pexEntry.getAddress(), pexEntry.getPort());
    }
  }

  /**
   * Find number of contracted peers union the number of connected peer
   */
  private int getKnownPeerCount() {
    Set<String> contractedPeers = contractManager.getContractedPeers();
    Set<String> connectedPeers = connectionManager.getAllConnectedPeerIds();
    Set<String> trackerPeers = trackerManager.getTrackerIds();

    return getKnownPeerCount(contractedPeers, connectedPeers, friendPeers, trackerPeers);
  }

  /**
   * Initiates connections to contracted, unconnected peers with pex info.
   */
  private void connectToContractedPeers(Set<String> contractedPeers, Set<String> connectedPeers) {
    Set<String> lostPeers = new HashSet<>();
    lostPeers.addAll(contractedPeers);
    lostPeers.removeAll(connectedPeers);

    // Try to reconnect to everyone based on missing pex information.
    for (String peerId : lostPeers) {
      try {
        PexEntry entry = pexManager.getEntry(peerId);
        connectionManager.connectToPeer(entry.getAddress(), entry.getPort());
      } catch (PexException e) {
        logger.trace("Contracted peer not found in PEX. Could not proactively connect. [{}]", peerId);
      }
    }
  }

  /**
   * Selects redundant peers and disconnects from them.
   */
  private void disconnectRedundantPeers(int peersToDisconnect) {
    logger.info("Disconnecting {} redundant peers.", peersToDisconnect);
    Set<String> contractedPeers = contractManager.getContractedPeers();

    connectionManager.getAllConnectedPeerIds().stream()
        .filter(x -> !contractedPeers.contains(x))
        .filter(x -> !trackerManager.getTrackerIds().contains(x))
        .limit(peersToDisconnect)
        .forEach(this::disconnectPeer);
  }

  /**
   * Disconnect from the given peer.
   *
   * @param peerId Peer to disconnect from
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

  public void setContractManager(ContractManager contractManager) {
    this.contractManager = contractManager;
    maintainPeerPopulation();
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


  /**
   * Total number of connections required to reach target for contract. Accounting for spare connections for contracted
   * and unconnected peers
   *
   * @return number of required connections. Can be negative.
   */
  static int requiredPeers(int knownPeerCount, int contractManagerTarget) {
    return contractManagerTarget - knownPeerCount;
  }

  /**
   * Total number of connections remaining.
   *
   * @return number of required connections. Can be negative.
   */
  static int remainingConnections(int knownPeerCount, int maxConnections) {
    return knownPeerCount - maxConnections;
  }

  /**
   * Find number of contracted peers union the number of connected peers
   *
   * @param contractedPeers Set of all contracted peers
   * @param connectedPeers  Set of all connected peers
   * @param friendPeers     Set of all friend peers
   * @param trackerIds      Set of all tracker peers
   * @return Number of peers contracted union those connected ignoring trackers.
   */
  static int getKnownPeerCount(Set<String> contractedPeers, Set<String> connectedPeers, Set<String> friendPeers, Set<String> trackerIds) {
    Set<String> knownPeers = new HashSet<>();
    knownPeers.addAll(contractedPeers);
    knownPeers.addAll(connectedPeers);
    knownPeers.addAll(friendPeers);
    knownPeers.removeAll(trackerIds);
    return knownPeers.size();
  }


}
