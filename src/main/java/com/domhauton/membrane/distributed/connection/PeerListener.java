package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import com.domhauton.membrane.distributed.connection.peer.Peer;
import com.domhauton.membrane.distributed.connection.peer.PeerException;
import com.domhauton.membrane.distributed.messaging.PeerMessage;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PemKeyCertOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private final Consumer<PeerMessage> peerMessageConsumer;

    public PeerListener(int port, Consumer<Peer> peerConsumer, Consumer<PeerMessage> peerMessageConsumer, MembraneAuthInfo membraneAuthInfo) {
        this.vertx = Vertx.vertx();
        this.port = port;
        this.peerConsumer = peerConsumer;
        this.peerMessageConsumer = peerMessageConsumer;

        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                .setCertValue(Buffer.buffer(membraneAuthInfo.getEncodedCert()))
                .setKeyValue(Buffer.buffer(membraneAuthInfo.getEncodedPrivateKey()));

        NetServerOptions netServerOptions = new NetServerOptions()
                .setPort(port)
                .setReceiveBufferSize(1024 * 1024 * RECIEVE_BUFFER_MB)
                .setPemKeyCertOptions(pemKeyCertOptions)
                .setSsl(true);
        server = vertx.createNetServer(netServerOptions);
        logger.info("TCP listening server set-up complete.");
    }

    public void start() {
        server.connectHandler(this::connectionHandler);
        server.listen(res -> {
            if (res.succeeded()) {
                logger.info("Listening for incoming TCP connections started on: {}", port);
            } else {
                logger.error("Failed to start server. {}", res.cause().getMessage());
            }
        });
    }

    private void connectionHandler(final NetSocket netSocket) {
        try {
            PeerConnection peerConnection = new PeerConnection(netSocket, peerMessageConsumer);
            peerConsumer.accept(new Peer(peerConnection));
        } catch (PeerException e) {
            logger.warn("Unable to accept connection from Peer. {} {}", e.getMessage(), netSocket.remoteAddress());
        }
    }

    public void close() {
        logger.info("Closing Peer Listener on port {}", port);
        server.close();
    }
}
