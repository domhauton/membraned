package com.domhauton.membrane.network.connection;

import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messages.PeerMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.PemKeyCertOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.interfaces.RSAPrivateKey;
import java.util.function.Consumer;

/**
 * Created by dominic on 09/02/17.
 */
class PeerDialler {

  private final Logger logger = LogManager.getLogger();
  private final static int BUFFER_MB = 256;

  private final Vertx vertx;
  private final NetClient client;
  private final RSAPrivateKey messageSigningKey;
  private final String localClientId;

  private final Consumer<Peer> peerConsumer;
  private final Consumer<PeerMessage> peerMessageConsumer;

  /**
   * Create a dialler that can call new peers.
   *
   * @param peerConsumer        Callback for connected peers.
   * @param peerMessageConsumer Connected peers will send messages here.
   * @param membraneAuthInfo    SSL Auth info to use while dialling.
   */
  PeerDialler(Consumer<Peer> peerConsumer, Consumer<PeerMessage> peerMessageConsumer, MembraneAuthInfo membraneAuthInfo) {
    vertx = Vertx.vertx();
    PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
            .setKeyValue(Buffer.buffer(membraneAuthInfo.getEncodedPrivateKey()))
            .setCertValue(Buffer.buffer(membraneAuthInfo.getEncodedCert()));

    NetClientOptions options = new NetClientOptions()
        .setLogActivity(true)
        .setPemKeyCertOptions(pemKeyCertOptions)
        .setTrustOptions(membraneAuthInfo.getTrustOptions())
        .setSsl(true)
        .setConnectTimeout(10000)
        .setReceiveBufferSize(BUFFER_MB * 1024 * 1024)
        .setSendBufferSize(BUFFER_MB * 1024 * 1024)
        .setReconnectAttempts(10)
        .setReconnectInterval(60 * 1000);

    client = vertx.createNetClient(options);
    this.peerConsumer = peerConsumer;
    this.peerMessageConsumer = peerMessageConsumer;
    this.messageSigningKey = membraneAuthInfo.getPrivateKey();
    this.localClientId = membraneAuthInfo.getClientId();
  }

  /**
   * Start attempting to establish connection to given client. Async.
   */
  void dialClient(String ip, int port) {
    client.connect(port, ip, this::connectionHandler);
  }

  /**
   * Handles new and failed connections.
   */
  private void connectionHandler(AsyncResult<NetSocket> result) {
    if (result.succeeded()) {
      NetSocket socket = result.result();
      try {
        logger.debug("Successfully Established TCP Link to new peer. [{}]. Converting to P2P Link.", socket.remoteAddress());
        PeerConnection peerConnection = new PeerConnection(socket, peerMessageConsumer, localClientId, messageSigningKey);
        peerConsumer.accept(new Peer(peerConnection));
      } catch (PeerException e) {
        logger.warn("Failed to connect: " + e.getMessage());
      }
    } else {
      logger.warn("Failed to connect! Reason: ", result.cause().getMessage() != null ? result.cause().getMessage() : "n/a");
    }
  }
}
