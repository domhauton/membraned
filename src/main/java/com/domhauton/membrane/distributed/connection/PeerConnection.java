package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.connection.peer.PeerException;
import com.domhauton.membrane.distributed.messaging.PeerMessageException;
import com.domhauton.membrane.distributed.messaging.PeerMessageUtils;
import com.domhauton.membrane.distributed.messaging.messages.PeerMessage;
import com.google.common.hash.Hashing;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.CertificateEncodingException;
import javax.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Created by dominic on 08/02/17.
 * <p>
 * Converts a NetSocket to a P2P connection that can be used by higher-level classes.
 */
public class PeerConnection {
  private final Logger logger = LogManager.getLogger();
  private final X509Certificate x509Certificate;
  private final String clientID;
  private final NetSocket netSocket;
  private final Consumer<PeerMessage> messageConsumer;
  private final DateTime connectionStartTime;

  private final CompletableFuture<Void> isClosed;

  /**
   * Establishes a P2P connection from a TCP connection. Ensures client presented one certificate for identity.
   *
   * @param netSocket       Socket to communicate over
   * @param messageConsumer Consumer for incoming messages
   * @throws PeerException If connection could not be converted to P2P.
   */
  PeerConnection(NetSocket netSocket, Consumer<PeerMessage> messageConsumer) throws PeerException {
    connectionStartTime = DateTime.now();
    this.netSocket = netSocket;
    this.isClosed = new CompletableFuture<>();
    this.messageConsumer = messageConsumer;
    this.netSocket.handler(this::messageHandler);
    this.netSocket.closeHandler((Void x) -> isClosed.complete(null));
    try {
      X509Certificate[] certificates = netSocket.peerCertificateChain();
      if (certificates != null && certificates.length == 1) {
        x509Certificate = certificates[0];
        clientID = Hashing.md5().hashBytes(x509Certificate.getEncoded()).toString();
      } else {
        logger.error("Certificate count invalid for {}. Dropping connection.", netSocket.localAddress());
        netSocket.close();
        throw new PeerException("Connection had incorrect number of certificates. Dropping");
      }
    } catch (SSLPeerUnverifiedException e) {
      logger.error("Connection unverified. Dropping. {}", e.getMessage());
      netSocket.close();
      throw new PeerException("Connection could not be verified. Dropping. " + e.getMessage());
    } catch (CertificateEncodingException e) {
      logger.error("Invalid certificated received. Could not generate client ID");
      netSocket.close();
      throw new PeerException("Connection could not parse certificate. Dropping.");
    }
    logger.info("Successfully Established P2P Link to {}", netSocket.remoteAddress());
  }

  /**
   * Sends message to the given peer. Async.
   *
   * @throws PeerException If message buffer was full.
   */
  public void sendMessage(PeerMessage peerMessage) throws PeerException {
    try {
      Buffer writeBuffer = Buffer.buffer(PeerMessageUtils.message2Bytes(peerMessage));
      if (!netSocket.writeQueueFull()) {
        logger.trace("Sending data from client [{}]: ", clientID, peerMessage);
        netSocket.write(writeBuffer);
      } else {
        throw new PeerException("Write queue was full.");
      }
    } catch (PeerMessageException e) {
      logger.error("Could not send message. Problem converting to JSON. {}", e.getMessage());
      throw new PeerException("Could not send message. Problem converting to JSON. " + e.getMessage());
    }

  }

  private void messageHandler(Buffer buffer) {
    try{
      PeerMessage peerMessage = PeerMessageUtils.bytes2Message(buffer.getBytes());
      //FIXME Check ID is correct
      messageConsumer.accept(peerMessage);
    } catch (PeerMessageException e) {
      logger.error("Could not receive message. Problem converting from JSON. IGNORING. {}", e.getMessage());
    }
  }

  public String getIP() {
    return netSocket.remoteAddress().host();
  }

  public int getPort() {
    return netSocket.remoteAddress().port();
  }

  public String getClientID() {
    return clientID;
  }

  public DateTime getConnectionStartTime() {
    return connectionStartTime;
  }

  public boolean isClosed() {
    return isClosed.isDone();
  }

  public synchronized void close() {
    netSocket.close();
  }
}
