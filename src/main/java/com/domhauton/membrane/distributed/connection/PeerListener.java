package com.domhauton.membrane.distributed.connection;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by dominic on 08/02/17.
 */
public class PeerListener {
    private final Logger logger = LogManager.getLogger();

    private final Vertx vertx;
    private final int port;
    private final NetServer server;

    public PeerListener(int port) {
        this.vertx = Vertx.vertx();
        this.port = port;
        NetServerOptions netServerOptions = new NetServerOptions().setPort(port);
        server = vertx.createNetServer(netServerOptions);
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

    public void connectionHandler(NetSocket netSocket) {

    }

    public void close() {
        server.close();
    }
}
