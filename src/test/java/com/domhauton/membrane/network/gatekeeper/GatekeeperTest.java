package com.domhauton.membrane.network.gatekeeper;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.pex.PexManager;
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
  private Gatekeeper gatekeeper;

  @BeforeEach
  void setUp() {
    connectionManager = Mockito.mock(ConnectionManager.class);
    contractManager = Mockito.mock(ContractManager.class);
    pexManager = Mockito.mock(PexManager.class);
    portForwardingService = Mockito.mock(PortForwardingService.class);
    gatekeeper = new Gatekeeper(connectionManager, contractManager, pexManager, portForwardingService, 5);
  }

  @Test
  void noActiveRequestsTest() {

  }
}