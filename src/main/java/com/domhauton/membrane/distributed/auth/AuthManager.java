package com.domhauton.membrane.distributed.auth;

import org.joda.time.DateTime;

import java.security.*;

/**
 * Created by dominic on 09/02/17.
 */
public class AuthManager {

    public void genKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair keypair = keyGen.generateKeyPair();
            PrivateKey privKey = keypair.getPrivate();
            PublicKey pubKey = keypair.getPublic();

            DateTime expiry = DateTime.now().plusDays(365);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
