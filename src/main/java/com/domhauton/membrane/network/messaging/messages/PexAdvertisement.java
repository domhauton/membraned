package com.domhauton.membrane.network.messaging.messages;

import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.auth.AuthUtils;
import org.joda.time.DateTime;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;

/**
 * Created by dominic on 31/03/17.
 */
public class PexAdvertisement extends PeerMessage {
  private String ip;
  private int port;
  private boolean isPublic;
  private DateTime dateTime;
  private byte[] signature;

  private PexAdvertisement() {
  } // For Jackson only!

  public PexAdvertisement(String ip, int port, boolean isPublic, DateTime dateTime) {
    super();
    this.ip = ip;
    this.port = port;
    this.isPublic = isPublic;
    this.dateTime = dateTime;
  }

  @Override
  public void executeAction(PeerMessageActions peerMessageActions) {

  }

  @Override
  public void sign(RSAPrivateKey rsaPrivateKey) throws AuthException {
    signature = AuthUtils.signMessage(rsaPrivateKey, generateSignedMessage());
  }

  @Override
  public boolean verify(X509Certificate x509Certificate) throws AuthException {
    return AuthUtils.verifySignedMessage(x509Certificate, generateSignedMessage());
  }

  private String generateSignedMessage() {
    return ip + port + isPublic + dateTime.toString();
  }

  @Override
  public String toString() {
    return "PexAdvertisement{" +
        "ip='" + ip + '\'' +
        ", port=" + port +
        ", isPublic=" + isPublic +
        ", sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
        ", dateTime=" + dateTime +
        ", messageId=" + messageId +
        ", responseToMessageId=" + responseToMessageId +
        ", signature=" + Arrays.toString(signature) +
        ", version='" + version + '\'' +
        '}';
  }
}
