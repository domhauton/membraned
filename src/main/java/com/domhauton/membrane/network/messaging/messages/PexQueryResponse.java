package com.domhauton.membrane.network.messaging.messages;

import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.messaging.PeerMessageException;
import com.google.common.net.InetAddresses;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by dominic on 31/03/17.
 */
public class PexQueryResponse extends PeerMessage {
  private final static Logger LOGGER = LogManager.getLogger();

  private Set<PexQueryResponseEntry> pexQueryResponseEntries;
  private Set<PexQueryResponseSignedEntry> pexQueryResponseSignedEntries;

  private PexQueryResponse() {
  } // For Jackson only!

  public PexQueryResponse(Set<PexQueryResponseEntry> pexQueryResponseEntries, Set<PexQueryResponseSignedEntry> pexQueryResponseSignedEntries) {
    this.pexQueryResponseEntries = pexQueryResponseEntries;
    this.pexQueryResponseSignedEntries = pexQueryResponseSignedEntries;
  }

  @Override
  public void executeAction(PeerMessageActions peerMessageActions) {
    processSignedEntries(peerMessageActions);
    processUnsignedEntries(peerMessageActions);
  }

  private void processSignedEntries(PeerMessageActions peerMessageActions) {
    for (PexQueryResponseSignedEntry entry : pexQueryResponseSignedEntries) {
      PexAdvertisement pexAdvertisement = new PexAdvertisement(entry.getIp(), entry.getPort(), entry.isPublic, entry.dateTime);
      pexAdvertisement.setSender(entry.userId);
      pexAdvertisement.setSignature(entry.signature);
      try {
        X509Certificate peerX509Certificate = peerMessageActions.retrievePeerCertificate(entry.userId);
        pexAdvertisement.verify(peerX509Certificate);
        pexAdvertisement.executeAction(peerMessageActions);
      } catch (PeerMessageException e) {
        LOGGER.trace("Invalid pex entry provided. Discarding. {}", e.getMessage());
      } catch (AuthException e) {
        LOGGER.trace("Unable to verify signed entry peer provided. Discarding. {}", e.getMessage());
      }
    }
  }

  private void processUnsignedEntries(PeerMessageActions peerMessageActions) {
    for (PexQueryResponseEntry entry : pexQueryResponseEntries) {
      if (0 < entry.getPort() && entry.getPort() < 65535) {
        LOGGER.trace("Discarding Pex Advertisement. Port out of bounds. {}", entry.getPort());
      } else if (!InetAddresses.isInetAddress(entry.getIp())) {
        LOGGER.trace("Discarding Pex Advertisement. Not valid IP. {}", entry.getIp());
      } else {
        peerMessageActions.processUnsignedPexInfo(entry.getIp(), entry.getPort());
      }
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

  @Override
  public String toString() {
    return "PexQueryResponse{" +
        "sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
        ", messageId=" + messageId +
        ", responseToMessageId=" + responseToMessageId +
        ", version='" + version + '\'' +
        ", pexQueryResponseEntries=" + pexQueryResponseEntries +
        ", pexQueryResponseSignedEntries=" + pexQueryResponseSignedEntries +
        '}';
  }

  class PexQueryResponseEntry {
    private String ip;
    private int port;

    PexQueryResponseEntry() {
    } // For Jackson Only

    PexQueryResponseEntry(String ip, int port) {
      this.ip = ip;
      this.port = port;
    }

    String getIp() {
      return ip;
    }

    int getPort() {
      return port;
    }

    @Override
    public String toString() {
      return "PexQueryResponseEntry{" +
          "ip='" + ip + '\'' +
          ", port=" + port +
          '}';
    }
  }

  class PexQueryResponseSignedEntry extends PexQueryResponseEntry {
    private String userId;
    private boolean isPublic;
    private DateTime dateTime;
    private byte[] signature;

    private PexQueryResponseSignedEntry() {
    } // For Jackson Only

    public PexQueryResponseSignedEntry(String ip, int port, String userId, boolean isPublic, DateTime dateTime, byte[] signature) {
      super(ip, port);
      this.userId = userId;
      this.isPublic = isPublic;
      this.dateTime = dateTime;
      this.signature = signature;
    }

    @Override
    public String toString() {
      return "PexQueryResponseSignedEntry{" +
          "ip='" + getIp() + '\'' +
          ", port=" + getPort() +
          ", userId='" + userId + '\'' +
          ", isPublic=" + isPublic +
          ", dateTime=" + dateTime +
          ", signature=" + Arrays.toString(signature) +
          '}';
    }
  }
}
