package com.domhauton.membrane.distributed.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dominic on 14/02/17.
 */
public class ReloadableTrustManagerFactorySpi extends TrustManagerFactorySpi {

    public ReloadableTrustManagerFactorySpi() throws NoSuchAlgorithmException {
        try {
            engineInit((KeyStore) null);
        } catch (KeyStoreException e) {
            throw new NoSuchAlgorithmException(e);
        }
    }

    private Logger logger = LogManager.getLogger();
    private TrustManager[] trustManagers;

    @Override
    protected void engineInit(KeyStore keyStore) throws KeyStoreException {
        try {
            logger.info("Initialising Reloadable Trust Manager");
            TrustManager trustManager = new ReloadableX509TrustManager(null);
            trustManagers = new TrustManager[]{trustManager};
        } catch (AuthException e) {
            logger.error("Failed to initialise Reloadable Trust Manager");
            throw new KeyStoreException(e);
        }
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException {
        try {
            this.engineInit((KeyStore) null);
        } catch (KeyStoreException e) {
            throw new InvalidAlgorithmParameterException(e);
        }
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        logger.info("Getting Reloadable Trust Manager");
        return trustManagers;
    }
}
