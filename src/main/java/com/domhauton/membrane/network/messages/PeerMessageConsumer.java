package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.RejectingContractManager;
import com.domhauton.membrane.network.Gatekeeper;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.pex.PexManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * Created by dominic on 06/04/17.
 */
public class PeerMessageConsumer implements Consumer<PeerMessage> {

  private final Logger logger = LogManager.getLogger();

  private final PeerMessageActionProvider actionProvider;

  public PeerMessageConsumer(ConnectionManager connectionManager, PexManager pexManager, Gatekeeper gatekeeper, PeerCertManager peerCertManager) {
    actionProvider = new PeerMessageActionProvider(connectionManager, pexManager, gatekeeper, peerCertManager, new RejectingContractManager());
  }

  @Override
  public void accept(PeerMessage peerMessage) {
    logger.trace("Message subscriber received message Sender {} ID {}", peerMessage.getSender(), peerMessage.getMessageId());
    peerMessage.executeAction(actionProvider);
  }

  public void setContractManager(ContractManager contractManager) {
    actionProvider.setContractManager(contractManager);
  }
}
