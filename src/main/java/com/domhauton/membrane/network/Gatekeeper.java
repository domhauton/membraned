package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.ContractManagerImpl;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

  Gatekeeper(ConnectionManager connectionManager, PexManager pexManager, PortForwardingService portForwardingService, PeerCertManager peerCertManager, TrackerManager trackerManager, int maxConnections) {
    this.connectionManager = connectionManager;
    this.pexManager = pexManager;
    this.maxConnections = maxConnections;
    this.portForwardingService = portForwardingService;
    this.peerCertManager = peerCertManager;
    this.trackerManager = trackerManager;

    contractManager = new ContractManagerImpl();
    friendPeers = new HashSet<>();
    peerMaintainerExecutor = Executors.newSingleThreadScheduledExecutor();
    startUpDateTime = DateTime.now();
  }

  /**
   * Attempt to connect to new peers, spread PEX information and disconnect redundant peers.
   */
  void maintainPeerPopulation() {
    logger.info("Running peer maintenance.");
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

    logger.info("{} more peer connections for contracts. {} to connect to more peers. Should {} connect to new public peers",
        requireMoreConnections ? "Require" : "Do not require",
        haveRemainingConnections ? "Able" : "Unable",
        searchForNewPublicPeers ? "" : "not");

    Collection<Peer> connectedPeerSet = connectionManager.getAllConnectedPeers();

    if (requireMoreConnections && haveRemainingConnections) {
      PexManager.requestPexInformation(connectedPeerSet, contractedPeers, searchForNewPublicPeers);
      if (searchForNewPublicPeers) {
        pexManager.connectToPublicPeersInPex(connectionManager, requiredConnections);
      }
    }

    boolean isPexUpdatePublic = searchForNewPublicPeers && requireMoreConnections;

    ExternalAddress externalAddress = portForwardingService.getExternalAddress();
    logger.debug("Sending external address {}:{}", externalAddress.getIpAddress(), externalAddress.getPort());
    PexManager.sendPexUpdate(externalAddress, connectedPeerSet, isPexUpdatePublic);

    // Disconnect from peers taking up useful connection slots

    if (remainingConnectionCount < 0) {
      logger.debug("Need to disconnect redundant peers. Too many connections.");
      disconnectRedundantPeers(-remainingConnectionCount, contractedPeers, connectedPeerSet);
    }

    // Connect to a tracker if struggling for peers.

    int contractedPeerTarget = contractManager == null ? 0 : contractManager.getContractCountTarget();
    int connectedPeerCount = connectedPeerSet.size();
    int minutesFromStartup = Minutes.minutesBetween(startUpDateTime, DateTime.now()).getMinutes();

    if (trackerManager.shouldConnectToTrackers(contractedPeerTarget, minutesFromStartup, connectedPeerCount)) {
      logger.debug("Connecting to tracker to assist locating more peers.");
      trackerManager.connectToTrackers(connectionManager);
    }
  }

  /**
   * Inform gatekeeper of new peer. May trigger creation of new contract
   *
   * @param peer New peer connected.
   */
  void processNewPeerConnected(Peer peer) {
    boolean isContracted = contractManager.getContractedPeers().contains(peer.getUid());
    // If is contracted or need new peers
    if (isContracted) {
      logger.info("New contracted peer connected. [{}]", peer.getUid());
      setupPeerContract(peer);
    } else {
      logger.debug("New non-contracted peer connected. [{}]", peer.getUid());

      boolean peerRequired = requiredPeers(getKnownPeerCount(), contractManager.getContractCountTarget()) > 0;

      logger.debug("Assessing peer viability for contract. {}{}[{}]",
          peerRequired ? "" : "No more contracted peer required. ",
          searchForNewPublicPeers ? "" : "Not searching for new public peers. ",
          peer.getUid());

      if (peerRequired && searchForNewPublicPeers) {
        logger.info("Contracting new peer. [{}]", peer.getUid());
        setupPeerContract(peer);
      }
    }
  }

  /**
   * Persist important peer information and inform contract manager of new peer.
   *
   * @param peer Peer to persist
   */
  private void setupPeerContract(Peer peer) {
    try {
      // Add contracted peer first. Order important.
      contractManager.addContractedPeer(peer.getUid());
      peerCertManager.addCertificate(peer.getUid(), peer.getX509Certificate());

      logger.debug("Sending PEX update to contracted Peer");
      ExternalAddress externalAddress = portForwardingService.getExternalAddress();
      PexManager.sendPexUpdate(externalAddress, Collections.singleton(peer), false);
    } catch (DistributorException e) {
      logger.warn("Unable to contract new peer.");
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
    if (!peerConnected) {
      logger.info("Unconnected peer pex entry received. [{}]", peerId);
      boolean isContracted = contractManager.getContractedPeers().contains(peerId);

      if (isContracted) {
        logger.trace("Peer in PEX entry contracted. Connecting. [{}]", peerId);
        connectionManager.connectToPeer(pexEntry.getAddress(), pexEntry.getPort());
      } else {

        int knownPeers = getKnownPeerCount();
        boolean newPeersRequired = requiredPeers(knownPeers, contractManager.getContractCountTarget()) > 0;
        boolean connectionsRemaining = remainingConnections(knownPeers, maxConnections) > 0;
        logger.trace("Checking if need more peers. {}{}{}",
            newPeersRequired ? "" : "No New Peers Required. ",
            connectionsRemaining ? "" : "No Connections Remaining. ",
            searchForNewPublicPeers ? "" : "Not Searching for new peers.");
        if (searchForNewPublicPeers && newPeersRequired && connectionsRemaining) {
          logger.debug("Connecting to new public peer.");
          connectionManager.connectToPeer(pexEntry.getAddress(), pexEntry.getPort());
        } else {
          logger.debug("Ignoring PEX entry for new public peer.");
        }
      }
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
    logger.info("Attempting to restore connection to {} contracted peers.", contractedPeers.size());
    Set<String> lostPeers = new HashSet<>();
    lostPeers.addAll(contractedPeers);
    lostPeers.removeAll(connectedPeers);
    logger.debug("Found {} disconnected contracted peers.", lostPeers.size());

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
  private void disconnectRedundantPeers(int peersToDisconnect, Set<String> contractedPeers, Collection<Peer> connectedPeerSet) {
    logger.info("Disconnecting {} redundant peers.", peersToDisconnect);

    long disconnectedPeerCount = connectedPeerSet.stream()
        .filter(x -> !contractedPeers.contains(x.getUid()))
        .filter(x -> !trackerManager.getTrackerIds().contains(x.getUid()))
        .peek(x -> logger.info("Disconnecting peer due to lack of connections. [{}]", x.getUid()))
        .limit(peersToDisconnect)
        .peek(Peer::close)
        .count();

    logger.info("{} of {} peers disconnected.", disconnectedPeerCount, peersToDisconnect);
  }

  /**
   * Set the contract manager to be used for establishing contracts.
   *
   * @param contractManager new contract manager to use.
   */
  void setContractManager(ContractManager contractManager) {
    this.contractManager = contractManager;
    // Reprocess all connected peers.
    connectionManager.getAllConnectedPeers().forEach(this::processNewPeerConnected);
  }

  /**
   * Allow new peers to connect and attempt to connect to them.
   *
   * @param searchForNewPublicPeers If true, allow new peer connections.
   */
  void setSearchForNewPublicPeers(boolean searchForNewPublicPeers) {
    this.searchForNewPublicPeers = searchForNewPublicPeers;
    // Reprocess all connected peers.
    connectionManager.getAllConnectedPeers().forEach(this::processNewPeerConnected);
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
  private static int requiredPeers(int knownPeerCount, int contractManagerTarget) {
    return contractManagerTarget - knownPeerCount;
  }

  /**
   * Total number of connections remaining.
   *
   * @return number of required connections. Can be negative.
   */
  private static int remainingConnections(int knownPeerCount, int maxConnections) {
    return maxConnections - knownPeerCount;
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
  private static int getKnownPeerCount(Set<String> contractedPeers, Set<String> connectedPeers, Set<String> friendPeers, Set<String> trackerIds) {
    Set<String> knownPeers = new HashSet<>();
    knownPeers.addAll(contractedPeers);
    knownPeers.addAll(connectedPeers);
    knownPeers.addAll(friendPeers);
    knownPeers.removeAll(trackerIds);
    return knownPeers.size();
  }


}
