package com.domhauton.membrane.network;

import com.domhauton.membrane.config.items.DistributedStorageConfig;
import com.domhauton.membrane.distributed.ContractManagerImpl;
import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.auth.AuthUtils;
import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.gatekeeper.Gatekeeper;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.upnp.ExternalAddress;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class NetworkManager implements Closeable {
  private final Logger logger = LogManager.getLogger();

  private final PortForwardingService portForwardingService;
  private final ConnectionManager connectionManager;
  private final PexManager pexManager;
  private final Gatekeeper gatekeeper;
  private final boolean monitorMode;


  public NetworkManager(Path authInfoPath, boolean monitorMode, DistributedStorageConfig config) throws NetworkException {
    MembraneAuthInfo membraneAuthInfo = loadAuthInfo(authInfoPath);
    this.connectionManager = new ConnectionManager(membraneAuthInfo, config.getTransportPort());

    this.portForwardingService = new PortForwardingService(x -> {
    }, config.getTransportPort());
    if (config.getExternalTransportPort() != -1) {
      portForwardingService.addNewMapping(config.getExternalTransportPort());
      portForwardingService.run();
    }
    this.pexManager = new PexManager(500, Paths.get(config.getStorageFolder()));
    this.gatekeeper = new Gatekeeper(connectionManager, new ContractManagerImpl(), pexManager, portForwardingService, 100);
    this.monitorMode = monitorMode;
  }

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

  public Set<ExternalAddress> getExternalIps() {
    return portForwardingService.getExternallyMappedAddresses();
  }

  public boolean peerConnected(String peerId) {
    return connectionManager.getAllConnectedPeers().stream()
            .anyMatch(x -> x.getUid().equals(peerId));
  }

  public void uploadBlockToPeer(String peerId, byte[] blockData) throws NetworkException {
    // TODO Implement
    throw new NetworkException("Block upload not implemented!");
  }

  public void close() {
    portForwardingService.close();
    connectionManager.close();
  }
}
