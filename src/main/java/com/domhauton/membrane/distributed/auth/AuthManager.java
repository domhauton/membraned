package com.domhauton.membrane.distributed.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;


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
}
