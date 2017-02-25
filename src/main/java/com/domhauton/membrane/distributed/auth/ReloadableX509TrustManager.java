package com.domhauton.membrane.distributed.auth;

import com.google.common.collect.EvictingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Created by dominic on 14/02/17.
 * <p>
 * Reloadable Trust Manager that will accept all certs and store new certs in-memory on connection.
 */
class ReloadableX509TrustManager implements X509TrustManager {
  private final Logger logger = LogManager.getLogger();
  private static final int CERT_STORE_SIZE = 100;

  private final Path keystorePath;
  private X509TrustManager trustManager;
  private final EvictingQueue<Certificate> tempCertList;

  ReloadableX509TrustManager(Path keyStorePath, Certificate initialCert) throws AuthException {
    this.keystorePath = keyStorePath;
    tempCertList = EvictingQueue.create(CERT_STORE_SIZE);
    tempCertList.add(initialCert);
    reloadTrustManager();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    try {
      trustManager.checkClientTrusted(chain, authType);
      logger.info("Client Cert Authentication Success.");
    } catch (CertificateException cx) {
      // Add new temporary certificate and check again.
      logger.trace("Client cert not present on host. Adding cert to trust store.");
      addCertAndReload(chain[0]);
      trustManager.checkClientTrusted(chain, authType);
    }
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    try {
      trustManager.checkServerTrusted(chain, authType);
      logger.info("Server Cert Authentication Success.");
    } catch (CertificateException cx) {
      // Add new temporary certificate and check again.
      logger.info("Server Cert Authentication Failed. Adding cert to trust store.");
      addCertAndReload(chain[0]);
      trustManager.checkServerTrusted(chain, authType);
    }
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return trustManager.getAcceptedIssuers();
  }

  private void reloadTrustManager() throws AuthException {
    logger.debug("Reloading cert trust store with {} temp certs", tempCertList.size());
    try {
      KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
      ts.load(null, null);
      for (Certificate cert : tempCertList) {
        ts.setCertificateEntry(UUID.randomUUID().toString(), cert);
      }
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(ts);
      trustManager = Stream.of(tmf.getTrustManagers())
              .filter((TrustManager trustManager) -> trustManager instanceof X509TrustManager)
              .map((TrustManager trustManager) -> (X509TrustManager) trustManager)
              .findAny()
              .orElseThrow(() -> new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory"));
    } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
      logger.error("Failed to reload trust store {}", e.getMessage());
      throw new AuthException("Could not reload trust store. " + e.getMessage(), e);
    }
  }

  private void addCertAndReload(Certificate cert) {
    tempCertList.add(cert);
    try {
      reloadTrustManager();
    } catch (AuthException e) {
      logger.error("Failed to add new cert. Authentication will fail.");
    }
  }
}
