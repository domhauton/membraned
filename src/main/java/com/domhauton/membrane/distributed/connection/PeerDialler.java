package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import com.domhauton.membrane.distributed.connection.peer.Peer;
import com.domhauton.membrane.distributed.connection.peer.PeerException;
import com.domhauton.membrane.distributed.messaging.PeerMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private final Consumer<PeerMessage> peerMessageConsumer;

    public PeerDialler(Consumer<Peer> peerConsumer, Consumer<PeerMessage> peerMessageConsumer, MembraneAuthInfo membraneAuthInfo) {
        vertx = Vertx.vertx();
        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                .setKeyValue(Buffer.buffer(membraneAuthInfo.getEncodedPrivateKey()))
                .setCertValue(Buffer.buffer(membraneAuthInfo.getEncodedCert()));

        NetClientOptions options = new NetClientOptions()
                .setLogActivity(true)
                .setPemKeyCertOptions(pemKeyCertOptions)
                .setTrustAll(true)
                .setSsl(true);
                //.setConnectTimeout(10000)
                //.setHostnameVerificationAlgorithm("") // Disable hostname verification. Certs are self-signed.
                //.setReceiveBufferSize(RECIEVE_BUFFER_MB * 1024 * 1024)


        client = vertx.createNetClient(options);
        this.peerConsumer = peerConsumer;
        this.peerMessageConsumer = peerMessageConsumer;
    }

    public void dialClient(String ip, int port) {
        client.connect(port, ip, this::connectionHandler);
    }

    public void connectionHandler(AsyncResult<NetSocket> result) {
        if (result.succeeded()) {
            NetSocket socket = result.result();
            logger.info("Connection to peer established. {}", socket.remoteAddress());
            try {
                PeerConnection peerConnection = new PeerConnection(socket, peerMessageConsumer);
                logger.debug("Successfully configured connection to new peer. {}", socket.remoteAddress());
                peerConsumer.accept(new Peer(peerConnection));
            } catch (PeerException e) {
                logger.warn("Failed to connect: " + e.getMessage());
            }
        } else {
            logger.warn("Failed to connect! Reason: ", result.cause().getMessage() == null ? result.cause().getMessage() : "n/a");
        }
    }
}
