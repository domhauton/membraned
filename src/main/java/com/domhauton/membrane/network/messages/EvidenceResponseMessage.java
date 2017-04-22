package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.distributed.evidence.EvidenceResponse;
import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.messages.data.EvidenceResponseEntry;
import org.joda.time.DateTime;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 31/03/17.
 */
public class EvidenceResponseMessage extends PeerMessage {

  private long responseTimeMillis;
  private Set<EvidenceResponseEntry> evidenceResponseEntries;

  private EvidenceResponseMessage() {
  } // For Jackson only!

  public EvidenceResponseMessage(DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
    this.responseTimeMillis = dateTime.getMillis();
    this.evidenceResponseEntries = evidenceResponses.stream()
        .map(x -> new EvidenceResponseEntry(x.getBlockId(), x.getEvidenceType(), x.getResponse()))
        .collect(Collectors.toSet());
  }

  @Override
  public void executeAction(PeerMessageActionProvider peerMessageActionProvider) {
    if (evidenceResponseEntries == null) {
      evidenceResponseEntries = Collections.emptySet();
    }
    DateTime dateTime = new DateTime(Math.max(0, responseTimeMillis));
    Set<EvidenceResponse> evidenceResponses = evidenceResponseEntries.stream()
        .map(x -> new EvidenceResponse(x.getBlockId(), x.getEvidenceType(), x.getResponse()))
        .collect(Collectors.toSet());
    peerMessageActionProvider.processEvidenceResponse(getSender(), dateTime, evidenceResponses);
  }

  @Override
  public void sign(RSAPrivateKey rsaPrivateKey) throws AuthException {
    // No way to sign
  }

  @Override
  public void verify(X509Certificate x509Certificate) throws AuthException, PeerMessageException {
    // No way to sign
  }

  public long getResponseTimeMillis() {
    return responseTimeMillis;
  }

  public Set<EvidenceResponseEntry> getEvidenceResponseEntries() {
    return evidenceResponseEntries;
  }

  @Override
  public String toString() {
    return "EvidenceResponseMessage{" +
        "responseTimeMillis=" + responseTimeMillis +
        ", sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
        ", evidenceResponseEntries (COUNT)=" + evidenceResponseEntries.size() +
        ", messageId=" + messageId +
        ", responseToMessageId=" + responseToMessageId +
        ", version='" + version + '\'' +
        '}';
  }
}
