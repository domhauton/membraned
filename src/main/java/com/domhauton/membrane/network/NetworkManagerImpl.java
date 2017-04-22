package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.auth.AuthUtils;
import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messages.ContractUpdateMessage;
import com.domhauton.membrane.network.messages.PeerMessageConsumer;
import com.domhauton.membrane.network.messages.PeerStorageBlock;
import com.domhauton.membrane.network.pex.PexException;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.tracker.TrackerManager;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class NetworkManagerImpl implements NetworkManager {
  private final Logger logger = LogManager.getLogger();

  private static final int MAX_LEDGER_SIZE = 500;
  private static final int MAX_SIMULTANEOUS_CONNECTIONS = 100;

  private final PortForwardingService portForwardingService;
  private final PeerCertManager peerCertManager;
  private final ConnectionManager connectionManager;
  private final PexManager pexManager;
  private final Gatekeeper gatekeeper;
  private final PeerMessageConsumer peerMessageConsumer;

  // Stored for test reflection.
  private final MembraneAuthInfo membraneAuthInfo;
  private final TrackerManager trackerManager;


  public NetworkManagerImpl(Path baseNetworkPath, int transportPort, int externalTransportPort) throws NetworkException {
    // Setup authentication information
    membraneAuthInfo = loadAuthInfo(baseNetworkPath); // Auto adds inner dir
    Path peerCertFolder = Paths.get(baseNetworkPath.toString() + File.separator + "peer");
    this.peerCertManager = new PeerCertManager(peerCertFolder);

    // Setup network connectivity
    this.connectionManager = new ConnectionManager(membraneAuthInfo, transportPort);
    this.portForwardingService = new PortForwardingService(transportPort);
    if (externalTransportPort > 0) {
      portForwardingService.addNewMapping(externalTransportPort);
    }

    // Setup peer info
    trackerManager = new TrackerManager();

    // Setup maintenance tasks
    this.pexManager = new PexManager(MAX_LEDGER_SIZE, baseNetworkPath);
    this.gatekeeper = new Gatekeeper(connectionManager, pexManager, portForwardingService,
        peerCertManager, trackerManager, MAX_SIMULTANEOUS_CONNECTIONS);

    // Configure message callback to all required network modules
    peerMessageConsumer = new PeerMessageConsumer(connectionManager, pexManager, gatekeeper, peerCertManager);
    connectionManager.registerMessageCallback(peerMessageConsumer);
    connectionManager.registerNewPeerCallback(gatekeeper::processNewPeerConnected);
  }

  /**
   * Create authentication info, from file or generate if unavailable
   *
   * @param authInfoPath Path auth info should be stored in
   * @return Generated auth info
   * @throws NetworkException Info could not be created
   */
  private MembraneAuthInfo loadAuthInfo(Path authInfoPath) throws NetworkException {
    try {
      logger.info("Loading auth info from [{}].", authInfoPath);
      return new MembraneAuthInfo(authInfoPath);
    } catch (AuthException e) {
      logger.warn("Could not load auth info from [{}]. Generating new auth info.", authInfoPath);
      try {
        AuthUtils.addProvider();
        MembraneAuthInfo tmpAuthInfo = AuthUtils.generateAuthenticationInfo();
        logger.info("Storing new auth info at [{}]", authInfoPath);
        tmpAuthInfo.write(authInfoPath);
        return tmpAuthInfo;
      } catch (AuthException e1) {
        logger.error("Could not generate new auth info. {}", e.getMessage());
        throw new NetworkException("Could not generate auth info. " + e.getMessage(), e1);
      } catch (IOException e1) {
        logger.error("Could not store new auth info. This is required for future login.");
        throw new NetworkException("Could not store new auth info. " + e.getMessage(), e1);
      }
    }
  }

  /**
   * Check if peer is connected
   *
   * @param peerId Id of the peer connected
   * @return true if peer is connected.
   */
  @Override
  public boolean peerConnected(String peerId) {
    return connectionManager.getAllConnectedPeers().stream()
        .anyMatch(x -> x.getUid().equals(peerId));
  }

  /**
   * Upload block to peer provided. Async with no confirmation of upload.
   *
   * @param peerId    Peer to upload to
   * @param blockData The bytes inside the block to upload
   * @throws NetworkException If there was an issue uploading. Peer not connected or buffer full.
   */
  @Override
  public void uploadBlockToPeer(String peerId, String blockId, byte[] blockData) throws NetworkException {
    try {
      Peer peerConnection = connectionManager.getPeerConnection(peerId, 2, TimeUnit.SECONDS);
      PeerStorageBlock peerStorageBlock = new PeerStorageBlock(blockId, blockData);
      peerConnection.sendPeerMessage(peerStorageBlock);
    } catch (TimeoutException e) {
      throw new NetworkException("Peer " + peerId + " unavailable.");
    } catch (PeerException e) {
      throw new NetworkException("Unable to send block to peer. [ " + peerId + " ]", e);
    }
  }

  @Override
  public void sendContractUpdateToPeer(String peerId, DateTime dateTime, int permittedBlockOffset, Set<String> storedBlockIds) throws NetworkException {
    try {
      logger.debug("Sending contract update to peer with {} permitted offset and {} stored blocks", permittedBlockOffset, storedBlockIds.size());
      Peer peerConnection = connectionManager.getPeerConnection(peerId, 2, TimeUnit.SECONDS);
      ContractUpdateMessage contractUpdateMessage = new ContractUpdateMessage(dateTime, permittedBlockOffset, storedBlockIds);
      peerConnection.sendPeerMessage(contractUpdateMessage);
    } catch (TimeoutException e) {
      throw new NetworkException("Peer " + peerId + " unavailable.");
    } catch (PeerException e) {
      throw new NetworkException("Unable to send contract update to peer. [ " + peerId + " ]", e);
    }
  }

  @Override
  public void setSearchForNewPublicPeers(boolean shouldSearch) {
    logger.info("Search for new public peers {}.", shouldSearch ? "enabled" : "disabled");
    gatekeeper.setSearchForNewPublicPeers(shouldSearch);
  }

  @Override
  public String getUID() {
    return membraneAuthInfo.getClientId();
  }

  @Override
  public String getPrivateEncryptionKey() {
    return membraneAuthInfo.getPrivateKey().getPrivateExponent().toString(Character.MAX_RADIX);
  }

  /**
   * Set the contract manager in all of the network subsystems
   *
   * @param contractManager ContractManager to set
   */
  @Override
  public void setContractManager(ContractManager contractManager) {
    peerMessageConsumer.setContractManager(contractManager);
    gatekeeper.setContractManager(contractManager);
    peerCertManager.setContractManager(contractManager);
  }

  @Override
  public void run() {
    // Run all services
    gatekeeper.run();
    portForwardingService.run();
  }

  @Override
  public void close() {
    try {
      pexManager.saveLedger();
    } catch (PexException e) {
      logger.error("Failed to save PEX info before shutdown");
    }
    gatekeeper.close();
    portForwardingService.close();
    connectionManager.close();
  }
}
