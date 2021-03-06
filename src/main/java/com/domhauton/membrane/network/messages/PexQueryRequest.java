package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.auth.AuthException;
import com.google.common.collect.ImmutableSet;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Set;

/**
 * Created by dominic on 31/03/17.
 */
public class PexQueryRequest extends PeerMessage {
  private Set<String> requestedPeers;
  private boolean requestPublic;

  private PexQueryRequest() {
  } // For Jackson only!

  public PexQueryRequest(Set<String> requestedPeers, boolean requestPublic) {
    this.requestedPeers = requestedPeers;
    this.requestPublic = requestPublic;
  }

  @Override
  public void executeAction(PeerMessageActionProvider peerMessageActionProvider) {
    peerMessageActionProvider.processPexRequest(getSender(), requestedPeers, requestPublic);
  }

  @Override
  public void sign(RSAPrivateKey rsaPrivateKey) throws AuthException {
    // No way to sign
  }

  @Override
  public void verify(X509Certificate x509Certificate) throws AuthException, PeerMessageException {
    // No way to sign
  }

  public Set<String> getRequestedPeers() {
    return ImmutableSet.copyOf(requestedPeers);
  }

  public boolean isRequestPublic() {
    return requestPublic;
  }

  @Override
  public String toString() {
    return "PexQueryRequest{" +
        "sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
        ", messageId=" + messageId +
        ", responseToMessageId=" + responseToMessageId +
        ", version='" + version + '\'' +
        ", requestedPeers=" + requestedPeers +
        ", requestPublic=" + requestPublic +
        '}';
  }
}
