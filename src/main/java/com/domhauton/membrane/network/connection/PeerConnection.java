package com.domhauton.membrane.network.connection;

import com.domhauton.membrane.MembraneBuild;
import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.auth.AuthUtils;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messages.PeerMessage;
import com.domhauton.membrane.network.messages.PeerMessageException;
import com.domhauton.membrane.network.messages.PeerMessageUtils;
import com.google.common.collect.EvictingQueue;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.Certificate;
import javax.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Created by dominic on 08/02/17.
 * <p>
 * Converts a NetSocket to a P2P connection that can be used by higher-level classes.
 */
public class PeerConnection {
  private final static int EVICTING_QUEUE_SIZE = 100;
  private final static long MAX_BLOCK_BUFFER_SIZE_BYTES = 96 * 1024 * 1024;
  private final int WRITE_QUEUE_MAX_SIZE_BYTES = 256 * 1024 * 1024;

  private final Logger logger = LogManager.getLogger();
  private final X509Certificate x509Certificate;
  private final String clientID;
  private final NetSocket netSocket;
  private final Consumer<PeerMessage> messageConsumer;
  private AtomicLong mostRecentCommunication;

  private final RSAPrivateKey messageSigningKey;
  private final String localClientId;

  private EvictingQueue<Long> mostRecentResponseIds;
  private Lock responseWaitLock;
  private Condition responseWaitCondition;

  private final AtomicLong currentSendId;

  private Boolean isClosed = false;

  private Buffer messageBuffer;

  /**
   * Establishes a P2P connection from a TCP connection. Ensures client presented one certificate for identity.
   *
   * @param netSocket       Socket to communicate over
   * @param messageConsumer Consumer for incoming messages
   * @throws PeerException If connection could not be converted to P2P.
   */
  PeerConnection(NetSocket netSocket, Consumer<PeerMessage> messageConsumer, String localClientId, RSAPrivateKey messageSigningKey) throws PeerException {
    this.netSocket = netSocket;
    this.messageConsumer = messageConsumer;
    this.netSocket.handler(this::messageHandler);
    this.netSocket.closeHandler(x -> isClosed = true);
    try {
      // n.b.: Certificates in deprecated javax format!
      Certificate[] certificates = netSocket.peerCertificateChain();
      if (certificates != null && certificates.length == 1) {
        // n.b.: Convert to new java format here.
        x509Certificate = AuthUtils.convertToX509Cert(certificates[0]);
        clientID = AuthUtils.certToPeerId(x509Certificate);
      } else {
        logger.error("Certificate count invalid for {}. Dropping connection.", netSocket.localAddress());
        netSocket.close();
        throw new PeerException("Connection had incorrect number of certificates. Dropping");
      }
    } catch (SSLPeerUnverifiedException e) {
      logger.error("Connection unverified. Dropping. {}", e.getMessage());
      netSocket.close();
      throw new PeerException("Connection could not be verified. Dropping. " + e.getMessage());
    } catch (CertificateEncodingException | CertificateException e) {
      logger.error("Invalid certificated received. Could not generate client ID");
      netSocket.close();
      throw new PeerException("Connection could not toEncryptedBytes certificate. Dropping.");
    }

    if (clientID.equalsIgnoreCase(localClientId)) { // You tried to connect to yourself?
      throw new PeerException("Cannot establish connection to yourself.");
    }

    mostRecentCommunication = new AtomicLong(System.currentTimeMillis());
    responseWaitLock = new ReentrantLock();
    responseWaitCondition = responseWaitLock.newCondition();
    mostRecentResponseIds = EvictingQueue.create(EVICTING_QUEUE_SIZE);

    currentSendId = new AtomicLong(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE));

    logger.info("Successfully Established P2P Link to {}", netSocket.remoteAddress());

    netSocket.setWriteQueueMaxSize(WRITE_QUEUE_MAX_SIZE_BYTES);
    // Will not be able to send signed messages until this step
    this.messageSigningKey = messageSigningKey;
    this.localClientId = localClientId;
  }

  /**
   * Sends message to the given peer. Async.
   *
   * @throws PeerException If message buffer was full.
   */
  public long sendMessage(PeerMessage peerMessage) throws PeerException {
    try {
      if (!netSocket.writeQueueFull()) {
        peerMessage.setSender(localClientId);
        peerMessage.setRecipient(getClientID());
        peerMessage.setVersion(MembraneBuild.VERSION);
        final long id = currentSendId.getAndUpdate(l -> Math.max(++l, 0L));
        logger.trace("Sending data from client [{}] [ID{}]: {}", clientID, id, peerMessage);
        peerMessage.setMessageId(id);
        peerMessage.sign(messageSigningKey);

        Buffer writeBuffer = Buffer.buffer(PeerMessageUtils.message2Bytes(peerMessage));
        netSocket.write(writeBuffer);
        return id;
      } else {
        logger.warn("Write queue full or connection closed. Could not send message to [{}].", clientID);
        throw new PeerException("Write queue full or connection closed.");
      }
    } catch (PeerMessageException e) {
      logger.error("Could not send message. Problem converting to JSON. {}", e.getMessage());
      throw new PeerException("Could not send message. Problem converting to JSON. " + e.getMessage());
    } catch (AuthException e) {
      logger.error("Could not sign message. {}", e.getMessage());
      throw new PeerException("Could not sign message. " + e.getMessage());
    }
  }

  private void messageHandler(Buffer buffer) {
    responseWaitLock.lock();
    try {
      if (buffer.getByte(buffer.length() - 1) != (byte) '}') {
        if (messageBuffer == null) {
          messageBuffer = Buffer.buffer(buffer.getByteBuf());
          logger.debug("Building partial message.");
        } else if (messageBuffer.length() < MAX_BLOCK_BUFFER_SIZE_BYTES) {
          messageBuffer.appendBuffer(buffer);
          if (messageBuffer.length() % (16 * 1024 * 1024) == 0) {
            logger.debug("Building partial message. Length {}KB", messageBuffer.length() / 1024);
          }
        }

      } else {
        if (messageBuffer != null) {
          if (messageBuffer.length() < MAX_BLOCK_BUFFER_SIZE_BYTES) {
            messageBuffer.appendBuffer(buffer);
            buffer = messageBuffer;
            logger.info("Parsing multi-part message. Size {}KB", messageBuffer.length() / 1024);
          } else {
            logger.error("Received message over {}KB. Dropping.", MAX_BLOCK_BUFFER_SIZE_BYTES / 1024);
          }
        }
        PeerMessage peerMessage = PeerMessageUtils.bytes2Message(buffer.getBytes());
        messageBuffer = null;
        if (!peerMessage.getSender().equals(clientID)) {
          logger.warn("Dropping message from peer [{}]. Masquerading as id: [{}]", clientID, peerMessage.getSender());
        } else {
          // Will throw if invalid.
          peerMessage.verify(x509Certificate);
          mostRecentCommunication.set(System.currentTimeMillis());
          long responseMessageId = peerMessage.getResponseToMessageId();
          if (responseMessageId != -1) {
            mostRecentResponseIds.add(responseMessageId);
            responseWaitCondition.signalAll();
            logger.trace("Signalled all about new message. Cached Ids: {}", mostRecentResponseIds);
          }
          messageConsumer.accept(peerMessage);
        }
      }
    } catch (PeerMessageException e) {
      logger.error("Invalid message. Ignoring. {}", e.getMessage());
    } catch (AuthException e) {
      logger.error("Could not verify message. IGNORING. {}", e.getMessage());
    } catch (Exception e) {
      logger.error("Corrupt message received. Throws unexpected exception during parsing. IGNORING. {}", e.getMessage());
      e.printStackTrace();
    } finally {
      responseWaitLock.unlock();
    }
  }

  /**
   * Will block until a response equal to the id arrives.
   *
   * @param responseId id of the required response
   * @param waitTime   time to wait for response
   * @param timeUnit   time unit of wait time
   * @throws TimeoutException If responseId not received in time.
   */
  public void waitForReply(long responseId, long waitTime, TimeUnit timeUnit) throws TimeoutException {
    try {
      Boolean foundResponse = CompletableFuture.supplyAsync(() -> waitForReply(responseId))
          .get(waitTime, timeUnit);
      if (!foundResponse) {
        throw new TimeoutException("Interrupted while waiting for response.");
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new TimeoutException("Timeout before message " + responseId + " arrived.");
    }
  }

  /**
   * Blocks until a message with id is found.
   *
   * @param responseId the responseId to wait for
   * @return message or
   */
  private boolean waitForReply(long responseId) {
    boolean foundResponse = false;
    logger.trace("Reply waiter waiting for lock.");
    responseWaitLock.lock();
    logger.trace("Reply waiter got lock.");
    try {
      logger.trace("Scanning {} for {}", mostRecentResponseIds, responseId);
      foundResponse = mostRecentResponseIds.stream()
          .anyMatch(val -> val == responseId);
      while (!foundResponse) {
        responseWaitCondition.await();
        logger.trace("Rescanning {} for {}", mostRecentResponseIds, responseId);
        foundResponse = mostRecentResponseIds.stream()
            .anyMatch(val -> val == responseId);
      }
    } catch (InterruptedException e) {
      logger.error("Interrupted during wait for peer [{}] message [{}]", clientID, responseId);
    } finally {
      responseWaitLock.unlock();
    }
    return foundResponse;
  }

  public X509Certificate getX509Certificate() {
    return x509Certificate;
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

  public boolean isClosed() {
    return isClosed;
  }

  public synchronized void close() {
    logger.info("Closing connection to [{}]", clientID);
    netSocket.close();
  }
}
