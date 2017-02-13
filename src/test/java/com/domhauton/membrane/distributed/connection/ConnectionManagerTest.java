package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.auth.AuthUtils;
import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by dominic on 13/02/17.
 */
class ConnectionManagerTest {
    private MembraneAuthInfo membraneAuthInfo1;
    private ConnectionManager connectionManager;

    @BeforeEach
    void setUp() throws Exception {
        membraneAuthInfo1 = AuthUtils.generateAuthenticationInfo();
    }

    @Test
    void listenAndClose() throws Exception {
        connectionManager = new ConnectionManager(membraneAuthInfo1, 12450);
        connectionManager.close();
    }
}