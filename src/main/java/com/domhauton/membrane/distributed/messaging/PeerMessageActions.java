package com.domhauton.membrane.distributed.messaging;

import com.domhauton.membrane.MembraneBuild;
import com.domhauton.membrane.distributed.connection.ConnectionException;
import com.domhauton.membrane.distributed.connection.ConnectionManager;
import com.domhauton.membrane.distributed.connection.peer.Peer;
import com.domhauton.membrane.distributed.connection.peer.PeerException;
import com.domhauton.membrane.distributed.messaging.messages.PongMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Dominic Hauton on 26/02/17.
 */
public class PeerMessageActions {
  private final static Logger logger = LogManager.getLogger();
  private final ConnectionManager connectionManager;
  private final String userID;

  public PeerMessageActions(ConnectionManager connectionManager, String userID) {
    this.connectionManager = connectionManager;
    this.userID = userID;
  }

  void sendPong(String targetUser) {
    try {
      Peer peer = connectionManager.getPeerConnection(targetUser);
      peer.sendPeerMessage(new PongMessage(userID, targetUser, MembraneBuild.VERSION));
    } catch (ConnectionException | PeerException e) {
      logger.warn("Unable to send pong to {}. {}", targetUser, e.getMessage());
    }
  }
}
