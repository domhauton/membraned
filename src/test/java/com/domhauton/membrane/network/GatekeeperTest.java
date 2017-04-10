package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.tracker.TrackerManager;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Created by dominic on 31/03/17.
 */
class GatekeeperTest {

  private ConnectionManager connectionManager;
  private ContractManager contractManager;
  private PexManager pexManager;
  private PortForwardingService portForwardingService;
  private PeerCertManager peerCertManager;
  private Gatekeeper gatekeeper;
  private TrackerManager trackerManager;

  @BeforeEach
  void setUp() {
    connectionManager = Mockito.mock(ConnectionManager.class);
    contractManager = Mockito.mock(ContractManager.class);
    pexManager = Mockito.mock(PexManager.class);
    portForwardingService = Mockito.mock(PortForwardingService.class);
    peerCertManager = Mockito.mock(PeerCertManager.class);
    trackerManager = new TrackerManager();
    gatekeeper = new Gatekeeper(connectionManager, contractManager, pexManager, portForwardingService, peerCertManager, trackerManager, 5);
  }

  @Test
  void noActiveRequestsTest() {

  }
}