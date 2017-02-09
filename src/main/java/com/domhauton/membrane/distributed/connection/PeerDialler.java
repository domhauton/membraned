package com.domhauton.membrane.distributed.connection;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * Created by dominic on 09/02/17.
 */
public class PeerDialler {

    private final Logger logger = LogManager.getLogger();

    private final Vertx vertx;
    private final NetClient client;

    private final Consumer<PeerConnection> peerConnectionConsumer;

    public PeerDialler(Consumer<PeerConnection> peerConnectionConsumer) {
        vertx = Vertx.vertx();
        NetClientOptions options = new NetClientOptions().setConnectTimeout(10000);
        client = vertx.createNetClient(options);
        this.peerConnectionConsumer = peerConnectionConsumer;
    }

    public void dialClient(String ip, int port) {
        client.connect(port, ip, this::connectionHandler);
    }

    public void connectionHandler(AsyncResult<NetSocket> result) {
        if (result.succeeded()) {
            logger.info("Connected!");
            NetSocket socket = result.result();
            PeerConnection peerConnection = new PeerConnection(socket);
            peerConnectionConsumer.accept(peerConnection);
        } else {
            logger.warn("Failed to connect: " + result.cause().getMessage());
        }
    }
}
