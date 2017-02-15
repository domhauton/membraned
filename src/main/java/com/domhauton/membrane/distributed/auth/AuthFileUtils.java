package com.domhauton.membrane.distributed.auth;

/**
 * Created by dominic on 11/02/17.
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;

public abstract class AuthFileUtils {
  private static final Logger logger = LogManager.getLogger();

  static void writePublicKey(Path filename, RSAPublicKey key) throws IOException {
    logger.info("Writing public key to [{}]", filename);
    write(filename, key);
  }

  static void writePrivateKey(Path filename, RSAPrivateKey key) throws IOException {
    logger.info("Writing private key to [{}]", filename);
    write(filename, key);
  }

  static void writeCertificate(Path filename, X509Certificate certificate) throws IOException {
    logger.info("Writing certificate to [{}]", filename);
    write(filename, certificate);
  }

  private static void write(Path filename, Object pemObject) throws IOException {
    boolean directoriesCreated = filename.toFile().getParentFile().mkdirs();
    if (directoriesCreated) {
      logger.info("Created auth directories for [{}]", filename.getParent());
    }
    try (
            FileOutputStream fileOutputStream = new FileOutputStream(filename.toFile());
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            JcaPEMWriter pemWriter = new JcaPEMWriter(outputStreamWriter)) {
      pemWriter.writeObject(pemObject);
    }
  }

  static X509Certificate loadCertificate(Path filePath) throws AuthException {
    logger.trace("Loading certificate key from [{}]", filePath);
    try (BufferedReader bufferedReader = Files.newBufferedReader(filePath)) {
      X509CertificateHolder x509CertificateHolder = (X509CertificateHolder) new PEMParser(bufferedReader).readObject();
      if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        logger.trace("Adding Bouncy Castle Provider.");
        Security.addProvider(new BouncyCastleProvider());
      }
      return new JcaX509CertificateConverter()
              .setProvider(BouncyCastleProvider.PROVIDER_NAME)
              .getCertificate(x509CertificateHolder);
    } catch (IOException | CertificateException | ClassCastException e) {
      logger.warn("Failed to read certificate. {}", e.getMessage());
      throw new AuthException("Could not read certificate. " + e.getMessage());
    }
  }

  static RSAPrivateKey loadPrivateKey(Path filePath) throws AuthException {
    logger.trace("Loading private key from [{}]", filePath);
    try (BufferedReader bufferedReader = Files.newBufferedReader(filePath)) {
      PEMParser pemParser = new PEMParser(bufferedReader);
      PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
      KeyPair kp = new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
      return (RSAPrivateKey) kp.getPrivate();
    } catch (IOException | ClassCastException e) {
      logger.warn("Failed to read private key. {}", e.getMessage());
      throw new AuthException("Could not read private key. " + e.getMessage());
    }
  }

  static RSAPublicKey loadPublicKey(Path filePath) throws AuthException {
    logger.trace("Loading public key from [{}]", filePath);
    try (BufferedReader bufferedReader = Files.newBufferedReader(filePath)) {
      PEMParser pemParser = new PEMParser(bufferedReader);
      SubjectPublicKeyInfo subjectPublicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
      RSAKeyParameters rsaKeyParameters = (RSAKeyParameters) PublicKeyFactory.createKey(subjectPublicKeyInfo);
      PKCS1Encoding cipher = new PKCS1Encoding(new RSAEngine());
      KeySpec ks = new RSAPublicKeySpec(rsaKeyParameters.getModulus(), rsaKeyParameters.getExponent());
      return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(ks);
    } catch (IOException | ClassCastException | InvalidKeySpecException | NoSuchAlgorithmException e) {
      logger.warn("Failed to read private key. {}", e.getMessage());
      throw new AuthException("Could not read private key. " + e.getMessage());
    }
  }
}