package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.distributed.evidence.EvidenceRequest;
import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.messages.data.EvidenceRequestEntry;
import org.joda.time.DateTime;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 31/03/17.
 */
public class EvidenceRequestMessage extends PeerMessage {

  private long requestTimeMillis;
  private Set<EvidenceRequestEntry> evidenceRequestEntries;

  private EvidenceRequestMessage() {
  } // For Jackson only!

  EvidenceRequestMessage(DateTime dateTime, Set<EvidenceRequest> evidenceRequests) {
    this.requestTimeMillis = dateTime.getMillis();
    this.evidenceRequestEntries = evidenceRequests.stream()
        .map(x -> new EvidenceRequestEntry(x.getBlockId(), x.getEvidenceType(), x.getSalt()))
        .collect(Collectors.toSet());
  }

  @Override
  public void executeAction(PeerMessageActionProvider peerMessageActionProvider) {
    if (evidenceRequestEntries != null) {
      evidenceRequestEntries = Collections.emptySet();
    }
    DateTime dateTime = new DateTime(Math.max(0, requestTimeMillis));
    Set<EvidenceRequest> evidenceRequests = evidenceRequestEntries.stream()
        .map(x -> new EvidenceRequest(x.getBlockId(), x.getEvidenceType(), x.getSalt()))
        .collect(Collectors.toSet());
    peerMessageActionProvider.processEvidenceRequests(getSender(), dateTime, evidenceRequests);
  }

  @Override
  public void sign(RSAPrivateKey rsaPrivateKey) throws AuthException {
    // No way to sign
  }

  @Override
  public void verify(X509Certificate x509Certificate) throws AuthException, PeerMessageException {
    // No way to sign
  }

  public long getRequestTimeMillis() {
    return requestTimeMillis;
  }

  public Set<EvidenceRequestEntry> getEvidenceRequestEntries() {
    return evidenceRequestEntries;
  }

  @Override
  public String toString() {
    return "EvidenceRequestMessage{" +
        "sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
        ", messageId=" + messageId +
        ", requestTimeMillis=" + requestTimeMillis +
        ", responseToMessageId=" + responseToMessageId +
        ", version='" + version + '\'' +
        ", evidenceRequestEntries (COUNT)=" + evidenceRequestEntries.size() +
        '}';
  }
}
