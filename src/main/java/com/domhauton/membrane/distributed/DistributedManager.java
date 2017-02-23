package com.domhauton.membrane.distributed;

import com.domhauton.membrane.config.items.DistributedStorageConfig;
import com.domhauton.membrane.distributed.auth.AuthException;
import com.domhauton.membrane.distributed.auth.AuthUtils;
import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import com.domhauton.membrane.distributed.connection.ConnectionException;
import com.domhauton.membrane.distributed.connection.ConnectionManager;
import com.domhauton.membrane.distributed.connection.upnp.PortForwardingService;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class DistributedManager implements Closeable {
  private final Logger logger = LogManager.getLogger();

  private final Optional<PortForwardingService> portForwardingServiceOptional;
  private final ConnectionManager connectionManager;
  private final boolean monitorMode;


  private StorageManager storageManager;


  public DistributedManager(Path authInfoPath, boolean monitorMode, DistributedStorageConfig config) throws DistributedException, ConnectionException {
    MembraneAuthInfo membraneAuthInfo = loadAuthInfo(authInfoPath);
    connectionManager = new ConnectionManager(membraneAuthInfo, config.getTransportPort());

    portForwardingServiceOptional =  config.isNatForwardingEnabled() ?
            Optional.of(new PortForwardingService(x -> {})) : Optional.empty();
    portForwardingServiceOptional.ifPresent(
            service -> service.addNewMapping(config.getTransportPort(), config.getExternalTransportPort()));
    portForwardingServiceOptional.ifPresent(PortForwardingService::run);
    this.monitorMode = monitorMode;
  }

  private MembraneAuthInfo loadAuthInfo(Path authInfoPath) throws DistributedException {
    try {
      logger.info("Loading auth info from [{}].", authInfoPath);
      return new MembraneAuthInfo(authInfoPath);
    } catch (AuthException e) {
      logger.warn("Could not load auth info from [{}]. Generating new auth info.", authInfoPath);
      try {
        MembraneAuthInfo tmpAuthInfo = AuthUtils.generateAuthenticationInfo();
        logger.info("Storing new auth info at [{}]", authInfoPath);
        tmpAuthInfo.write(authInfoPath);
        return tmpAuthInfo;
      } catch (AuthException e1) {
        logger.error("Could not generate new auth info. {}", e.getMessage());
        throw new DistributedException("Could not generate auth info. " + e.getMessage(), e1);
      } catch (IOException e1) {
        logger.error("Could not store new auth info. This is required for future login.");
        throw new DistributedException("Could not store new auth info. " + e.getMessage(), e1);
      }
    }
  }

  public void setStorageManager(StorageManager storageManager) throws DistributedException {
    if(this.storageManager != null) {
      throw new DistributedException("Storage Manager can only be set once.");
    }
    this.storageManager = storageManager;
  }

  public void close() {
    try {
      this.storageManager.close();
    } catch (StorageManagerException e) {
      logger.fatal("Could not close storage manager correctly. Unrecoverable. {}", e.getMessage());
    }
    portForwardingServiceOptional.ifPresent(PortForwardingService::close);
    connectionManager.close();
  }
}
