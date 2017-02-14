package com.domhauton.membrane.distributed.auth;

import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.TrustManagerFactory;

/**
 * Created by dominic on 14/02/17.
 */
public class ReloadableTrustOptions implements TrustOptions {
    private final Logger logger = LogManager.getLogger();

    @Override
    public TrustManagerFactory getTrustManagerFactory(Vertx vertx) throws Exception {
        logger.debug("Creating custom Reloadable Trust Manager Factory.");
        return new ReloadableTrustManagerFactory();
    }

    @Override
    public TrustOptions clone() {
        return new ReloadableTrustOptions();
    }
}
