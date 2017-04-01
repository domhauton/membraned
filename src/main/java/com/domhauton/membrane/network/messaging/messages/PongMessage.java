package com.domhauton.membrane.network.messaging.messages;

import com.domhauton.membrane.network.auth.AuthException;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

/**
 * Created by dominic on 12/02/17.
 */
public class PongMessage extends PeerMessage {

  private PongMessage() {} // For Jackson only!

  public PongMessage(long pingID) {
    super(pingID);
  }

  @Override
  public void executeAction(PeerMessageActions peerMessageActions) {

  }

  @Override
  public void sign(RSAPrivateKey rsaPrivateKey) throws AuthException {
    // Do not sign
  }

  @Override
  public void verify(X509Certificate x509Certificate) throws AuthException {
    // Nothing to verify
  }

  @Override
  public String toString() {
    return "PongMessage{" +
            "sender='" + sender + '\'' +
            ", recipient='" + recipient + '\'' +
            ", messageId=" + messageId +
            ", responseToMessageId=" + responseToMessageId +
            ", version='" + version + '\'' +
            '}';
  }
}
