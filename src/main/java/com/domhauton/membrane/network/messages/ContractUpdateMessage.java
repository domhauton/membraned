package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.auth.AuthException;
import org.joda.time.DateTime;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;
import java.util.Set;

/**
 * Created by dominic on 31/03/17.
 */
public class ContractUpdateMessage extends PeerMessage {

  private long updateTimeMillis;
  private int permittedBlockOffset;
  private Set<String> storedBlockIds;

  private ContractUpdateMessage() {
  } // For Jackson only!

  public ContractUpdateMessage(DateTime dateTime, int permittedBlockOffset, Set<String> storedBlockIds) {
    this.updateTimeMillis = dateTime.getMillis();
    this.permittedBlockOffset = permittedBlockOffset;
    this.storedBlockIds = storedBlockIds;
  }

  @Override
  public void executeAction(PeerMessageActionProvider peerMessageActionProvider) {
    if (storedBlockIds == null) {
      storedBlockIds = Collections.emptySet();
    }
    DateTime dateTime = new DateTime(Math.max(0, updateTimeMillis));
    permittedBlockOffset = Math.max(0, permittedBlockOffset);
    peerMessageActionProvider.processContractUpdate(getSender(), dateTime, permittedBlockOffset, storedBlockIds);
  }

  @Override
  public void sign(RSAPrivateKey rsaPrivateKey) throws AuthException {
    // No way to sign
  }

  @Override
  public void verify(X509Certificate x509Certificate) throws AuthException, PeerMessageException {
    // No way to sign
  }

  public long getUpdateTimeMillis() {
    return updateTimeMillis;
  }

  public int getPermittedBlockOffset() {
    return permittedBlockOffset;
  }

  public Set<String> getStoredBlockIds() {
    return storedBlockIds;
  }

  @Override
  public String toString() {
    return "ContractUpdateMessage{" +
        "updateTimeMillis=" + updateTimeMillis +
        ", permittedBlockOffset=" + permittedBlockOffset +
        ", storedBlockIds (COUNT)=" + storedBlockIds.size() +
        ", sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
        ", messageId=" + messageId +
        ", responseToMessageId=" + responseToMessageId +
        ", version='" + version + '\'' +
        '}';
  }
}
