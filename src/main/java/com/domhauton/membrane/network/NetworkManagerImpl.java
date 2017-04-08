package com.domhauton.membrane.network;

import com.domhauton.membrane.config.items.DistributedStorageConfig;
import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.ContractManagerImpl;
import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.auth.AuthUtils;
import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.gatekeeper.Gatekeeper;
import com.domhauton.membrane.network.messages.PeerMessageConsumer;
import com.domhauton.membrane.network.pex.PexException;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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


  public NetworkManagerImpl(Path authInfoPath, DistributedStorageConfig config) throws NetworkException {

    // Setup authentication information
    MembraneAuthInfo membraneAuthInfo = loadAuthInfo(authInfoPath);
    Path peerCertFolder = Paths.get(authInfoPath.toString() + File.separator + "/peer");
    this.peerCertManager = new PeerCertManager(peerCertFolder);

    // Setup network connectivity
    this.connectionManager = new ConnectionManager(membraneAuthInfo, config.getTransportPort());
    this.portForwardingService = new PortForwardingService(config.getTransportPort());
    if (config.getExternalTransportPort() < 0) {
      portForwardingService.addNewMapping(config.getExternalTransportPort());
    }

    // Setup maintenance tasks
    this.pexManager = new PexManager(MAX_LEDGER_SIZE, Paths.get(config.getStorageFolder()));
    this.gatekeeper = new Gatekeeper(connectionManager, new ContractManagerImpl(), pexManager, portForwardingService, peerCertManager, MAX_SIMULTANEOUS_CONNECTIONS);
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
  public void uploadBlockToPeer(String peerId, byte[] blockData) throws NetworkException {
    // TODO Implement
    throw new NetworkException("Block upload not implemented!");
  }

  /**
   * Set the contract manager in all of the network subsystems
   *
   * @param contractManager ContractManager to set
   */
  @Override
  public void setContractManager(ContractManager contractManager) {
    gatekeeper.setContractManager(contractManager);
    peerCertManager.setContractManager(contractManager);
  }

  @Override
  public void run() {
    // Configure message callback to all required network modules
    PeerMessageConsumer peerMessageConsumer = new PeerMessageConsumer(connectionManager, pexManager, gatekeeper, peerCertManager);
    connectionManager.registerMessageCallback(peerMessageConsumer);
    connectionManager.registerNewPeerCallback(gatekeeper::processNewPeerConnected);

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
