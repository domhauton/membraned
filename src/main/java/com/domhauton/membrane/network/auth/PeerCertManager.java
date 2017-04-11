package com.domhauton.membrane.network.auth;

import com.domhauton.membrane.distributed.ContractManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by dominic on 03/04/17.
 */
public class PeerCertManager {
  private final Logger logger = LogManager.getLogger();
  private final static String EXTENSION = ".crt";

  private final Path certStoreFolder;
  private final HashMap<String, X509Certificate> certificateMap;

  private ContractManager contractManager;

  public PeerCertManager(Path certStoreFolder) {
    this.certStoreFolder = certStoreFolder;
    this.certificateMap = loadCerts(certStoreFolder);

    try {
      if (!certStoreFolder.toFile().exists()) {
        Files.createDirectories(certStoreFolder);
      }
    } catch (IOException e) {
      logger.warn("Could not create folder for peer certificates!", e.getMessage());
    }
  }

  /**
   * Persist peer certificate. Ensure peer is contracted before addition.
   *
   * @param userId          ID of peer
   * @param x509Certificate Certificate to persist
   */
  public void addCertificate(String userId, X509Certificate x509Certificate) {
    certificateMap.put(userId, x509Certificate);
    Path certPath = getCertPath(userId);
    try {
      Files.deleteIfExists(certPath);
      AuthFileUtils.writeCertificate(certPath, x509Certificate);
    } catch (IOException e) {
      logger.warn("Failed to write certificate to file. [{}]", certPath);
    }

    // Only sweep certs if there is an active contract manager!

    getActiveContacts().ifPresent(this::removeNonContractedPeers);
  }

  private Path getCertPath(String userId) {
    return Paths.get(certStoreFolder.toString() + File.separator + userId + EXTENSION);
  }

  private HashMap<String, X509Certificate> loadCerts(Path certStoreFolder) {

    HashMap<String, X509Certificate> existingPeerCerts = new HashMap<>();

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(certStoreFolder)) {
      for (Path path : directoryStream) {
        if (path.toString().endsWith(EXTENSION)) {
          try {
            X509Certificate x509Certificate = AuthFileUtils.loadCertificate(path);
            String peerName = path.getFileName().toString().replace(EXTENSION, "");
            logger.debug("Loaded cert for user [{}]", peerName);
            existingPeerCerts.put(peerName, x509Certificate);
          } catch (AuthException e) {
            logger.error("Error loading cert [{}]", path);
          }
        }
      }
    } catch (IOException ex) {
      logger.warn("IOException while loading existing certs.", ex.getMessage());
    }
    return existingPeerCerts;
  }

  private void removeNonContractedPeers(Set<String> contractedPeers) {
    logger.info("Removing certificates for uncontracted peers.");
    Set<String> redundantPeerCerts = new HashSet<>(certificateMap.keySet());
    redundantPeerCerts.removeAll(contractedPeers);
    redundantPeerCerts.stream()
        .peek(certificateMap::remove)
        .map(this::getCertPath)
        .forEach(certPath -> {
          try {
            Files.deleteIfExists(certPath);
          } catch (IOException e) {
            logger.error("Failed to remove cert. [{}]", certPath);
          }
        });
    logger.info("Certificate Removal Complete. Removed {} certs.", redundantPeerCerts.size());
  }

  private Optional<Set<String>> getActiveContacts() {
    return contractManager == null ? Optional.empty() : Optional.of(contractManager.getContractedPeers());
  }

  /**
   * Set the contract manager used to determine active contract list.
   *
   * @param contractManager the new contract manager
   */
  public void setContractManager(ContractManager contractManager) {
    this.contractManager = contractManager;
  }

  /**
   * Retrieves the certificate for the given peer.
   *
   * @param peerId UID for the peer
   * @return Certificate request
   * @throws NoSuchElementException If certificate does not exist
   */
  public X509Certificate getCertificate(String peerId) throws NoSuchElementException {
    X509Certificate x509Certificate = certificateMap.get(peerId);
    if (x509Certificate != null) {
      return x509Certificate;
    } else {
      throw new NoSuchElementException("Certificate for " + peerId + " not stored.");
    }
  }
}
