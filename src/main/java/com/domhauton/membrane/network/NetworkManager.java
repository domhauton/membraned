package com.domhauton.membrane.network;

import com.domhauton.membrane.config.items.DistributedStorageConfig;
import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.auth.AuthUtils;
import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.connection.ConnectionException;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.upnp.PortForwardingService;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class NetworkManager implements Closeable {
  private final Logger logger = LogManager.getLogger();

  private final Optional<PortForwardingService> portForwardingServiceOptional;
  private final ConnectionManager connectionManager;
  private final boolean monitorMode;


  private StorageManager storageManager;


  public NetworkManager(Path authInfoPath, boolean monitorMode, DistributedStorageConfig config) throws NetworkException, ConnectionException {
    MembraneAuthInfo membraneAuthInfo = loadAuthInfo(authInfoPath);
    connectionManager = new ConnectionManager(membraneAuthInfo, config.getTransportPort());

    portForwardingServiceOptional =  config.isNatForwardingEnabled() ?
            Optional.of(new PortForwardingService(x -> {})) : Optional.empty();
    portForwardingServiceOptional.ifPresent(
            service -> service.addNewMapping(config.getTransportPort(), config.getExternalTransportPort()));
    portForwardingServiceOptional.ifPresent(PortForwardingService::run);
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

  public void setStorageManager(StorageManager storageManager) throws NetworkException {
    if(this.storageManager != null) {
      throw new NetworkException("Storage Manager can only be set once.");
    }
    this.storageManager = storageManager;
  }

  public boolean peerConnected(String peerId) {
    return connectionManager.getAllConnectedPeers().stream()
            .anyMatch(x -> x.getUid().equals(peerId));
  }

  public void addPeersToBlockList(Set<String> peersToBlock) {
    //TODO add peer block list.

    // TODO Check current PEX list.
    // TODO Attempt to connect to everyone on list in batches of 5 every 10 seconds until peer quota is met.

    // TODO If quota is not met, request PEX share from all connected peers.
    // TODO Open up to new random connection requests for next 15 mins.
  }

  public void close() {
    portForwardingServiceOptional.ifPresent(PortForwardingService::close);
    try {
      storageManager.close();
    } catch (StorageManagerException e) {
      logger.fatal("Could not close storage manager correctly. Unrecoverable. {}", e.getMessage());
    }
    connectionManager.close();
  }
}
