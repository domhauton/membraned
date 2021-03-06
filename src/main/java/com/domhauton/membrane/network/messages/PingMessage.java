package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.auth.AuthException;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

/**
 * Created by dominic on 12/02/17.
 */
public class PingMessage extends PeerMessage {
  public PingMessage() {
  }

  @Override
  public void executeAction(PeerMessageActionProvider peerMessageActionProvider) {
    peerMessageActionProvider.sendPongAsync(getSender(), getMessageId());
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
    return "PingMessage{" +
            "sender='" + sender + '\'' +
            ", recipient='" + recipient + '\'' +
            ", messageId=" + messageId +
            ", responseToMessageId=" + responseToMessageId +
            ", version='" + version + '\'' +
            '}';
  }
}
