package com.domhauton.membrane.distributed.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;


/**
 * Created by dominic on 09/02/17.
 */
public class AuthManager {
    private final Logger logger = LogManager.getLogger();

    private static final int KEY_SIZE = 2048;
    private static final String RSA_PUBIC_FILE_NAME = "membrane_rsa.pub";
    private static final String RSA_PUBIC_FILE_DESCRIPTION = "RSA PUBLIC KEY";

    private static final String RSA_PRIVATE_FILE_NAME = "membrane_rsa";
    private static final String RSA_PRIVATE_FILE_DESCRIPTION = "RSA PRIVATE KEY";

    private static final String CERT_FILE_NAME = "membrane.cert";
    private static final String RSA_CERT_FILE_DESCRIPTION = "CERTIFICATE";

    private static final String SIGNATURE_ALGORITHM = "SHA256WITHRSA";
//    private static final String KEY_STORE_TYPE = "JKS";


    public void genKey(Path path) throws AuthManagerException {
        try {
            KeyPair keyPair = generateRSAKeyPair();

            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            Path privKeyPath = getPrivateKeyPath(path);
            PemFile pemFile = new PemFile(privateKey, RSA_PRIVATE_FILE_DESCRIPTION);
            pemFile.write(privKeyPath);

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            Path pubKeyPath = getPublicKeyPath(path);
            PemFile publicPemFile = new PemFile(publicKey, RSA_PUBIC_FILE_DESCRIPTION);
            publicPemFile.write(pubKeyPath);

            Certificate x509Cert = generate(keyPair);
            Path certKeyPath = getCertPath(path);
            PemFile certPemFile = new PemFile(x509Cert);
            certPemFile.write(certKeyPath);
        } catch (NoSuchProviderException e) {
            logger.error("Bouncy Castle Encryption provider not found. {}", e.getMessage());
            throw new AuthManagerException("Bouncy Castle Encryption provider not found.");
        } catch (IOException e) {
            logger.error("Could not write RSA key to file. {}",  e.getMessage());
            throw new AuthManagerException("Could not write RSA key to file." + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Could not generate RSA key as algo not found. {}", e.getMessage());
            throw new AuthManagerException("Could not write RSA key to file." + e.getMessage());
        }
    }

    public void loadCertificate(Path path) {

    }

    public X509Certificate generate(KeyPair keyPair) throws AuthManagerException {
        // Start creating a self-signed X.509 certificate with the public key

        X500Name subjName = new X500Name("C=NA, ST=NA, O=Membrane, CN=membrane.domhauton.com");
        BigInteger serialNumber = new BigInteger("900");

        Date startDate = DateTime.now().minusHours(1).toDate();
        Date endDate = DateTime.now().plusYears(100).toDate();

        JcaX509v3CertificateBuilder x509Builder = new JcaX509v3CertificateBuilder(
                subjName,
                serialNumber,
                startDate,
                endDate,
                subjName,
                keyPair.getPublic());

        // Create a signer to sign (self-sign) the certificate.

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        try {
            ContentSigner signer = signerBuilder.build(keyPair.getPrivate());
            return converter.getCertificate(x509Builder.build(signer));
        } catch (OperatorCreationException e) {
            logger.error("Could not sign certificate {}", e.getMessage());
            throw new AuthManagerException("Could not sign certificate. " + e.getMessage());
        } catch (CertificateException e) {
            logger.error("Could not create certificate. {}", e.getMessage());
            throw new AuthManagerException("Could not create certificate. " + e.getMessage());
        }
    }

//    private void saveSelfSignedCert(KeyPair keyPair, X509Certificate certificate, char[] password, Path targetPath) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
//        KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE);
//        ks.load(null, password);
//
//        KeyStore.PrivateKeyEntry privKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new Certificate[] {certificate});
//        ks.setEntry("membraneRSAKey", privKeyEntry, new KeyStore.PasswordProtection(password));
//
//        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())){
//            ks.store(fos, password);
//        }
//    }

    private static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(KEY_SIZE);
        return generator.generateKeyPair();
    }

    private Path getPrivateKeyPath(Path path) {
        return Paths.get(path + File.separator + RSA_PRIVATE_FILE_NAME);
    }

    private Path getPublicKeyPath(Path path) {
        return Paths.get(path + File.separator + RSA_PUBIC_FILE_NAME);
    }

    private Path getCertPath(Path path) {
        return Paths.get(path + File.separator + CERT_FILE_NAME);
    }
}
