package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.auth.AuthUtils;
import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by dominic on 13/02/17.
 */
class ConnectionManagerTest {
    private Logger logger = LogManager.getLogger();

    private MembraneAuthInfo membraneAuthInfo1;
    private MembraneAuthInfo membraneAuthInfo2;
    private ConnectionManager connectionManager1;
    private ConnectionManager connectionManager2;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        //System.setProperty("javax.net.debug", "ssl");
        membraneAuthInfo1 = AuthUtils.generateAuthenticationInfo();
        membraneAuthInfo2 = AuthUtils.generateAuthenticationInfo();

        // Install the all-trusting trust manager
        //Path keystoreDir = Paths.get(System.getProperty("user.home") + File.separator + ".keystore");
        //Files.createDirectories(keystoreDir);
//        SSLContext sslContext = getSSLContext();
//        SSLContext.setDefault(sslContext);
//        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }

    @Test
    void listenAndClose() throws Exception {
        connectionManager1 = new ConnectionManager(membraneAuthInfo1, 12450);
        connectionManager1.close();
    }

    @Test
    void sendPing() throws Exception {
        connectionManager2 = new ConnectionManager(membraneAuthInfo1, 12451);

        connectionManager2.connectToPeer("127.0.0.1", 12450);

        Thread.sleep(5000);

        connectionManager2.close();
    }

    @Test
    void listenPing() throws Exception {
        connectionManager1 = new ConnectionManager(membraneAuthInfo1, 12450);

        Thread.sleep(50000);

        connectionManager1.close();
    }
}