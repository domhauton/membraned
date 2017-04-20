package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.auth.AuthException;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

/**
 * Created by dominic on 31/03/17.
 */
public class PeerStorageBlock extends PeerMessage {

  private String blockId;
  private byte[] blockData;

  private PeerStorageBlock() {
  } // For Jackson only!

  public PeerStorageBlock(String blockId, byte[] blockData) {
    this.blockId = blockId;
    this.blockData = blockData;
  }

  @Override
  public void executeAction(PeerMessageActionProvider peerMessageActionProvider) {
    if (blockId != null && blockData != null) {
      peerMessageActionProvider.processNewBlock(getSender(), blockId, blockData);
    } else {
      LOGGER.warn("Ignoring block from peer as data is incomplete. Peer: [{}]", getSender());
    }
  }

  @Override
  public void sign(RSAPrivateKey rsaPrivateKey) throws AuthException {
    // No way to sign
  }

  @Override
  public void verify(X509Certificate x509Certificate) throws AuthException, PeerMessageException {
    // No way to sign
  }

  public String getBlockId() {
    return blockId;
  }

  public byte[] getBlockData() {
    return blockData;
  }

  @Override
  public String toString() {
    return "PeerStorageBlock{" +
        "sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
        ", messageId=" + messageId +
        ", responseToMessageId=" + responseToMessageId +
        ", version='" + version + '\'' +
        ", blockId='" + blockId + '\'' +
        ", blockData=" + Base64.getEncoder().encodeToString(blockData) +
        '}';
  }
}
