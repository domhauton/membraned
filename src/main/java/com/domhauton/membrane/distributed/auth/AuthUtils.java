package com.domhauton.membrane.distributed.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;


/**
 * Created by dominic on 09/02/17.
 */
public abstract class AuthUtils {
  private static final Logger logger = LogManager.getLogger();
  private static final int KEY_SIZE = 2048;
  private static final String SIGNATURE_ALGORITHM = "SHA256WITHRSA";


  public static MembraneAuthInfo generateAuthenticationInfo() throws AuthException {
    try {
      logger.info("Generating RSA key pair using {}", SIGNATURE_ALGORITHM);
      KeyPair keyPair = generateRSAKeyPair();
      RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
      RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
      X509Certificate x509Cert = generate(keyPair);
      logger.info("Auth info successfully generated.");
      return new MembraneAuthInfo(x509Cert, publicKey, privateKey);
    } catch (NoSuchProviderException e) {
      logger.error("Bouncy Castle Encryption provider not found. {}", e.getMessage());
      throw new AuthException("Bouncy Castle Encryption provider not found.");
    } catch (NoSuchAlgorithmException e) {
      logger.error("Could not generate key as algo not found. {}", e.getMessage());
      throw new AuthException("Could not write RSA key to file." + e.getMessage());
    }
  }

  private static X509Certificate generate(KeyPair keyPair) throws AuthException {
    logger.info("Generating X.509 certificate.");
    // Start creating a self-signed X.509 certificate with the public key
    X500Name issuer = new X500NameBuilder()
            .addRDN(BCStyle.O, "Membrane Distributed Storage")
            .addRDN(BCStyle.CN, "Membrane")
            .build();
    // X500Name subjName = new X500Name("CN=SecureTrust CA, O=SecureTrust Corporation, C=US");
    BigInteger serial = BigInteger.valueOf(900);

    Date startDate = DateTime.now().minusHours(1).toDate();
    Date endDate = DateTime.now().plusYears(100).toDate();

    JcaX509v3CertificateBuilder x509Builder = new JcaX509v3CertificateBuilder(
            issuer,
            serial,
            startDate,
            endDate,
            issuer,
            keyPair.getPublic());

    // Create a signer to sign (self-sign) the certificate.

    JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM);
    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    try {
      ContentSigner signer = signerBuilder.build(keyPair.getPrivate());
      X509Certificate x509Certificate = converter.getCertificate(x509Builder.build(signer));
      logger.info("Successfully generated certificate.");
      return x509Certificate;
    } catch (OperatorCreationException e) {
      logger.error("Could not sign certificate {}", e.getMessage());
      throw new AuthException("Could not sign certificate. " + e.getMessage());
    } catch (CertificateException e) {
      logger.error("Could not create certificate. {}", e.getMessage());
      throw new AuthException("Could not create certificate. " + e.getMessage());
    }
  }

  private static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    logger.trace("Generating RSA pair");
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      logger.trace("Adding Bouncy Castle Provider.");
      Security.addProvider(new BouncyCastleProvider());
    }
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
    generator.initialize(KEY_SIZE);
    return generator.generateKeyPair();
  }


}
