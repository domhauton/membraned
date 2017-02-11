package com.domhauton.membrane.distributed.peer.connection;

import com.domhauton.membrane.distributed.peer.Peer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Created by dominic on 09/02/17.
 */
public class PeerDialler {

    private final Logger logger = LogManager.getLogger();
    private final static int RECIEVE_BUFFER_MB = 256;

    private final Vertx vertx;
    private final NetClient client;

    private final Consumer<Peer> peerConsumer;

    public PeerDialler(Consumer<Peer> peerConsumer, byte[] privateKey, byte[] cert) {
        vertx = Vertx.vertx();

        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                .setKeyValue(Buffer.buffer(privateKey))
                .setCertValue(Buffer.buffer(cert));

        NetClientOptions options = new NetClientOptions()
                .setConnectTimeout(10000)
                .setPemKeyCertOptions(pemKeyCertOptions)
                .setReceiveBufferSize(RECIEVE_BUFFER_MB * 1024 * 1024)
                .setSsl(true);

        client = vertx.createNetClient(options);
        this.peerConsumer = peerConsumer;
    }

    public void dialClient(String ip, int port) {
        client.connect(port, ip, this::connectionHandler);
    }

    public void connectionHandler(AsyncResult<NetSocket> result) {
        if (result.succeeded()) {
            logger.info("Connected!");
            NetSocket socket = result.result();
            PeerConnection peerConnection = new PeerConnection(socket);
            //TODO ensure connection authenticated.

            CompletableFuture.runAsync(peerConnection::authenticate)

        } else {
            logger.warn("Failed to connect: " + result.cause().getMessage());
        }
    }
}
