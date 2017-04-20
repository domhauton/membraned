package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.auth.AuthException;
import com.domhauton.membrane.network.messages.data.PexQueryResponseEntry;
import com.domhauton.membrane.network.messages.data.PexQueryResponseSignedEntry;
import com.google.common.net.InetAddresses;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Created by dominic on 31/03/17.
 */
public class PexQueryResponse extends PeerMessage {
  private Set<PexQueryResponseEntry> pexQueryResponseEntries;
  private Set<PexQueryResponseSignedEntry> pexQueryResponseSignedEntries;

  private PexQueryResponse() {
  } // For Jackson only!

  public PexQueryResponse(Set<PexQueryResponseEntry> pexQueryResponseEntries, Set<PexQueryResponseSignedEntry> pexQueryResponseSignedEntries) {
    this.pexQueryResponseEntries = pexQueryResponseEntries;
    this.pexQueryResponseSignedEntries = pexQueryResponseSignedEntries;
  }

  @Override
  public void executeAction(PeerMessageActionProvider peerMessageActionProvider) {
    processSignedEntries(peerMessageActionProvider);
    processUnsignedEntries(peerMessageActionProvider);
  }

  private void processSignedEntries(PeerMessageActionProvider peerMessageActionProvider) {
    for (PexQueryResponseSignedEntry entry : pexQueryResponseSignedEntries) {
      PexAdvertisement pexAdvertisement = new PexAdvertisement(entry.getIp(), entry.getPort(), entry.isPublic(), entry.getDateTime());
      pexAdvertisement.setSender(entry.getUserId());
      pexAdvertisement.setSignature(entry.getSignature());
      try {
        X509Certificate peerX509Certificate = peerMessageActionProvider.retrievePeerCertificate(entry.getUserId());
        pexAdvertisement.verify(peerX509Certificate);
        pexAdvertisement.executeAction(peerMessageActionProvider);
      } catch (PeerMessageException e) {
        LOGGER.trace("Invalid pex entry provided. Discarding. {}", e.getMessage());
      } catch (NoSuchElementException e) {
        LOGGER.trace("Unable to find cert for peer. Did not request. Discarding. {}", e.getMessage());
      } catch (AuthException e) {
        LOGGER.trace("Unable to verify signed entry peer provided. Discarding. {}", e.getMessage());
      }
    }
  }

  private void processUnsignedEntries(PeerMessageActionProvider peerMessageActionProvider) {
    for (PexQueryResponseEntry entry : pexQueryResponseEntries) {
      if (0 > entry.getPort() && entry.getPort() < 65535) {
        LOGGER.trace("Discarding Pex Advertisement. Port out of bounds. {}", entry.getPort());
      } else if (!InetAddresses.isInetAddress(entry.getIp())) {
        LOGGER.trace("Discarding Pex Advertisement. Not valid IP. {}", entry.getIp());
      } else {
        peerMessageActionProvider.processUnsignedPexInfo(entry.getIp(), entry.getPort());
      }
    }
  }

  public Set<PexQueryResponseEntry> getPexQueryResponseEntries() {
    return pexQueryResponseEntries;
  }

  public Set<PexQueryResponseSignedEntry> getPexQueryResponseSignedEntries() {
    return pexQueryResponseSignedEntries;
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
}
