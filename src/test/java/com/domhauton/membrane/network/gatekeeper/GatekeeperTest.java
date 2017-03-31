package com.domhauton.membrane.network.gatekeeper;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.pex.PexManager;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

/**
 * Created by dominic on 31/03/17.
 */
class GatekeeperTest {

  private ConnectionManager connectionManager;
  private ContractManager contractManager;
  private PexManager pexManager;
  private Gatekeeper gatekeeper;

  @BeforeEach
  void setUp() {
    connectionManager = Mockito.mock(ConnectionManager.class);
    contractManager = Mockito.mock(ContractManager.class);
    pexManager = Mockito.mock(PexManager.class);
    gatekeeper = new Gatekeeper(connectionManager, contractManager, pexManager, 5);
  }
}