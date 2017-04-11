package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.auth.AuthUtils;
import com.google.common.net.InetAddresses;
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
  private boolean publicInfo;
  private long dateTimeMillis;
  private byte[] signature;

  private PexAdvertisement() {
  } // For Jackson only!

  public PexAdvertisement(String ip, int port, boolean publicInfo, DateTime dateTime) {
    super();
    this.ip = ip;
    this.port = port;
    this.publicInfo = publicInfo;
    this.dateTimeMillis = dateTime.getMillis();
  }

  @Override
  public void executeAction(PeerMessageActionProvider peerMessageActionProvider) {
    peerMessageActionProvider.processSignedPexInfo(getSender(), ip, port, publicInfo, new DateTime(dateTimeMillis), signature);
  }

  @Override
  public void sign(RSAPrivateKey rsaPrivateKey) throws AuthException {
    signature = AuthUtils.signMessage(rsaPrivateKey, generateSignedMessage());
  }

  @Override
  public void verify(X509Certificate x509Certificate) throws AuthException, PeerMessageException {
    if (0 > port && port < 65535) {
      throw new PeerMessageException("Discarding Pex Advertisement. Port out of bounds. " + port);
    } else if (!InetAddresses.isInetAddress(ip)) {
      throw new PeerMessageException("Discarding Pex Advertisement. Not valid IP. " + ip);
    } else if (!new DateTime(dateTimeMillis).isAfter(DateTime.now().minusSeconds(15))) { // Allow 15s slip
      throw new PeerMessageException("Discarding Pex Advertisement. Signed for future date. " + new DateTime(dateTimeMillis));
    } else if (!AuthUtils.verifySignedMessage(x509Certificate, generateSignedMessage(), signature)) {
      throw new PeerMessageException("Discarding Pex Advertisement. Unable to verify signature.");
    }
  }

  void setSignature(byte[] signature) {
    this.signature = signature;
  }

  private String generateSignedMessage() {
    return ip + port + publicInfo + dateTimeMillis;
  }

  public String getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }

  public long getDateTimeMillis() {
    return dateTimeMillis;
  }

  public byte[] getSignature() {
    return signature;
  }

  public boolean isPublicInfo() {
    return publicInfo;
  }

  @Override
  public String toString() {
    return "PexAdvertisement{" +
        "sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
        ", ip='" + ip + '\'' +
        ", messageId=" + messageId +
        ", port=" + port +
        ", responseToMessageId=" + responseToMessageId +
        ", publicInfo=" + publicInfo +
        ", version='" + version + '\'' +
        ", dateTimeMillis=" + dateTimeMillis +
        ", signature=" + Arrays.toString(signature) +
        '}';
  }
}
