package com.domhauton.membrane.distributed.peer.connection;

import com.domhauton.membrane.distributed.peer.Peer;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PfxOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.security.tools.keytool.CertAndKeyGen;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.function.Consumer;

/**
 * Created by dominic on 08/02/17.
 */
public class PeerListener {
    private final Logger logger = LogManager.getLogger();
    private final static int RECIEVE_BUFFER_MB = 256;

    private final Vertx vertx;
    private final int port;
    private final NetServer server;

    private final Consumer<Peer> peerConsumer;

    public PeerListener(int port, Consumer<Peer> peerConsumer, ) {
        this.vertx = Vertx.vertx();
        this.port = port;
        this.peerConsumer = peerConsumer;

        PfxOptions pfxOptions = new PfxOptions().setValue();
        NetServerOptions netServerOptions = new NetServerOptions()
                .setPort(port)
                .setReceiveBufferSize(1024*1024*RECIEVE_BUFFER_MB)
                .setClientAuth(ClientAuth.REQUIRED)
                .setPemKeyCertOptions();
        server = vertx.createNetServer(netServerOptions);
        logger.info("TCP listening server set-up complete.");
    }

    public void start() {
        server.listen(res -> {
            if (res.succeeded()) {
                logger.info("Listening for incoming TCP connections started");
            } else {
                logger.error("Failed to start server. {}", res.cause().getMessage());
            }
        });

        server.connectHandler(this::connectionHandler);
    }

    public void connectionHandler(final NetSocket netSocket) {
        PeerConnection peerConnection = new PeerConnection(netSocket);
        // Ensure handshake
        peerConsumer.accept();
    }

    public void close() {
        server.close();
    }
}
