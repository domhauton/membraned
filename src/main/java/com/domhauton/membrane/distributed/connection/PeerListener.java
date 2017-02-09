package com.domhauton.membrane.distributed.connection;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * Created by dominic on 08/02/17.
 */
public class PeerListener {
    private final Logger logger = LogManager.getLogger();

    private final Vertx vertx;
    private final int port;
    private final NetServer server;

    private final Consumer<PeerConnection> peerConnectionConsumer;

    public PeerListener(int port, Consumer<PeerConnection> peerConnectionConsumer) {
        this.vertx = Vertx.vertx();
        this.port = port;
        this.peerConnectionConsumer = peerConnectionConsumer;
        NetServerOptions netServerOptions = new NetServerOptions().setPort(port);
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
        peerConnectionConsumer.accept(new PeerConnection(netSocket));
    }

    public void close() {
        server.close();
    }
}
