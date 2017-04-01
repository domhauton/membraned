package com.domhauton.membrane.network.connection;

import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messaging.messages.PeerMessage;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PemKeyCertOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.interfaces.RSAPrivateKey;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Created by dominic on 08/02/17.
 * <p>
 * Listens for incoming TCP connections.
 */
class PeerListener {
  private final Logger logger = LogManager.getLogger();
  private final static int RECEIVE_BUFFER_MB = 256;

  private final Vertx vertx;
  private final int port;
  private final NetServer server;

  private final RSAPrivateKey messageSigningKey;
  private final String localClientId;

  private final Consumer<Peer> peerConsumer;
  private final Consumer<PeerMessage> peerMessageConsumer;

  /**
   * Create a listener for incoming connections on the given port.
   *
   * @param peerConsumer        Callback for connected peers.
   * @param peerMessageConsumer Connected peers will send messages here.
   * @param membraneAuthInfo    SSL Auth info to use while dialling.
   */
  PeerListener(int port, Consumer<Peer> peerConsumer, Consumer<PeerMessage> peerMessageConsumer, MembraneAuthInfo membraneAuthInfo) {
    this.vertx = Vertx.vertx();
    this.port = port;
    this.peerConsumer = peerConsumer;
    this.peerMessageConsumer = peerMessageConsumer;

    PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
            .setCertValue(Buffer.buffer(membraneAuthInfo.getEncodedCert()))
            .setKeyValue(Buffer.buffer(membraneAuthInfo.getEncodedPrivateKey()));


    logger.info("TCP listening server set-up start.");
    NetServerOptions netServerOptions = new NetServerOptions()
            .setPort(port)
            .setLogActivity(true)
            .setClientAuth(ClientAuth.REQUIRED)
            .setSsl(true)
            .setTrustOptions(membraneAuthInfo.getTrustOptions())
            .setPemKeyCertOptions(pemKeyCertOptions)
            .setReceiveBufferSize(1024 * 1024 * RECEIVE_BUFFER_MB);

    server = vertx.createNetServer(netServerOptions);
    logger.info("TCP listening server set-up complete.");
    this.messageSigningKey = membraneAuthInfo.getPrivateKey();
    this.localClientId = membraneAuthInfo.getClientId();
  }

  /**
   * Start listening.
   */
  void start(CompletableFuture<Boolean> successCallback) {
    logger.info("Starting TCP Server");
    server.connectHandler(this::connectionHandler);
    server.listen(res -> {
      successCallback.complete(res.succeeded());
      if (res.succeeded()) {
        logger.info("Listening for incoming TCP connections started on: {}", port);
      } else {
        logger.error("Failed to start server. {}", res.cause().getMessage());
      }
    });
    logger.info("Starting TCP Server Completed.");
  }

  /**
   * Handles new successful connections.
   */
  private void connectionHandler(final NetSocket netSocket) {
    try {
      PeerConnection peerConnection = new PeerConnection(netSocket, peerMessageConsumer, localClientId, messageSigningKey);
      peerConsumer.accept(new Peer(peerConnection));
    } catch (PeerException e) {
      logger.warn("Unable to accept connection from Peer. {} {}", e.getMessage(), netSocket.remoteAddress());
    }
  }

  int getPort() {
    return port;
  }

  void close() {
    logger.info("Closing Peer Listener on port {}", port);
    server.close();
  }
}
