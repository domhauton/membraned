package com.domhauton.membrane.network.connection.peer;

import com.domhauton.membrane.network.connection.PeerConnection;
import com.domhauton.membrane.network.messaging.messages.PeerMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by dominic on 08/02/17.
 */
public class Peer implements Closeable {
  private final Logger logger = LogManager.getLogger();

  private final String uid;
  private final PeerConnection peerConnection;

  public Peer(PeerConnection peerConnection) {
    this.peerConnection = peerConnection;
    this.uid = peerConnection.getClientID();
  }

  public String getUid() {
    return uid;
  }

  /**
   * Send a message to the peer
   *
   * @param peerMessage Message to send
   * @return The assigned messageId for the message
   * @throws PeerException The send Buffer to the peer was full. Slow down.
   */
  public long sendPeerMessage(PeerMessage peerMessage) throws PeerException {
    return peerConnection.sendMessage(peerMessage);
  }

  /**
   * Send a message to the peer and wait for the response.
   *
   * @param peerMessage Message to send.
   * @param timeout     Time to wait for response to come back
   * @param timeUnit    Unit for @param timeout
   * @return The assigned messageId for the message.
   * @throws PeerException    The Buffer to the peer was full. Slow down.
   * @throws TimeoutException The response did not come back before the timeout
   */
  public long sendPeerMessageAndWait(PeerMessage peerMessage, long timeout, TimeUnit timeUnit) throws PeerException, TimeoutException {
    long sentMessageId = sendPeerMessage(peerMessage);
    logger.debug("Sent message with id {}. Waiting {} {} for response.", sentMessageId, timeout, timeUnit);
    peerConnection.waitForReply(sentMessageId, timeout, timeUnit);
    return sentMessageId;
  }

  public X509Certificate getX509Certificate() {
    return peerConnection.getX509Certificate();
  }

  public String getIP() {
    return peerConnection.getIP();
  }

  public boolean isClosed() {
    return peerConnection.isClosed();
  }

  public void close() {
    logger.info("Closing connection to peer [{}]", uid);
    peerConnection.close();
  }
}
