package com.domhauton.membrane.distributed.auth;

import javax.net.ssl.TrustManagerFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dominic on 14/02/17.
 */
public class ReloadableTrustManagerFactory extends TrustManagerFactory {
    public ReloadableTrustManagerFactory() throws NoSuchAlgorithmException {
        super(new ReloadableTrustManagerFactorySpi(),
                KeyPairGenerator.getInstance("RSA").getProvider(),
                KeyPairGenerator.getInstance("RSA").getAlgorithm());
    }
}
