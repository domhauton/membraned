package com.domhauton.membrane.network.auth;

import com.google.common.hash.Hashing;
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

import javax.security.cert.CertificateEncodingException;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
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
    } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
      logger.error("Bouncy Castle Encryption provider or algo not found. {}", e.getMessage());
      throw new AuthException("Bouncy Castle Encryption provider or RSA algo not found.");
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

    // Create a signer to build (self-build) the certificate.

    JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM);
    JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
    try {
      ContentSigner signer = signerBuilder.build(keyPair.getPrivate());
      X509Certificate x509Certificate = converter.getCertificate(x509Builder.build(signer));
      logger.info("Successfully generated certificate.");
      return x509Certificate;
    } catch (OperatorCreationException | CertificateException e) {
      logger.error("Could not build/create certificate {}", e.getMessage());
      throw new AuthException("Could not build/create certificate. " + e.getMessage());
    }
  }

  private static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    logger.trace("Generating RSA pair");
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
    generator.initialize(KEY_SIZE);
    return generator.generateKeyPair();
  }

  public static void addProvider() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      logger.trace("Adding Bouncy Castle Provider.");
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public synchronized static X509Certificate convertToX509Cert(javax.security.cert.Certificate certificate) throws CertificateEncodingException, CertificateException {
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    ByteArrayInputStream bais = new ByteArrayInputStream(certificate.getEncoded());
    return (X509Certificate) certificateFactory.generateCertificate(bais);
  }

  public static String certToPeerId(X509Certificate x509Certificate) {
    return Hashing.sha256().hashBytes(x509Certificate.getPublicKey().getEncoded()).toString();
  }

  public static byte[] signMessage(RSAPrivateKey privateKey, String message) throws AuthException {
    try {
      Signature signature = Signature.getInstance("SHA1withRSA", "BC");
      signature.initSign(privateKey);
      signature.update(message.getBytes());
      return signature.sign();
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new AuthException("Could not create signature. Invalid algo used! " + e.getMessage());
    } catch (InvalidKeyException e) {
      throw new AuthException("Could not create signature. Invalid key! " + e.getMessage());
    } catch (SignatureException e) {
      throw new AuthException("Unable to sign correctly. " + e.getMessage());
    }
  }

  public static boolean verifySignedMessage(X509Certificate certificate, String message, byte[] sigBytes) throws AuthException {
    try {
      Signature signature = Signature.getInstance("SHA1withRSA", "BC");
      signature.initVerify(certificate);
      signature.update(message.getBytes());
      return signature.verify(sigBytes);
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new AuthException("Could not create signature. Invalid algo used! " + e.getMessage());
    } catch (InvalidKeyException e) {
      throw new AuthException("Could not create signature. Invalid key! " + e.getMessage());
    } catch (SignatureException e) {
      throw new AuthException("Unable to sign correctly. " + e.getMessage());
    }
  }
}
