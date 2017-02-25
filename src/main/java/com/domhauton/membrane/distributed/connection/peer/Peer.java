package com.domhauton.membrane.distributed.connection.peer;

import com.domhauton.membrane.distributed.connection.PeerConnection;
import com.domhauton.membrane.distributed.messaging.messages.PeerMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;

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

  public void sendPeerMessage(PeerMessage peerMessage) throws PeerException {
    peerConnection.sendMessage(peerMessage);
  }

  public boolean isClosed() {
    return peerConnection.isClosed();
  }

  public void close() {
    logger.info("Closing connection to peer [{}]", uid);
    peerConnection.close();
  }
}
