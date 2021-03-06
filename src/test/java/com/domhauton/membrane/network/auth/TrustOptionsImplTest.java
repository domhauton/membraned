package com.domhauton.membrane.network.auth;

import io.vertx.core.net.TrustOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Created by Dominic Hauton on 16/02/17.
 *
 * Tests for TrustOptionsImpl
 */
public class TrustOptionsImplTest {

  @Test
  public void testReturnsSuppliedTrustManagers() throws Exception {
    Supplier<TrustManager[]> trustManSupplier = () -> new TrustManager[]{
            new DummyTestTrustManager("foobar1"),
            new DummyTestTrustManager("foobar2"),
            new DummyTestTrustManager("foobar3"),
    };

    TrustOptions trustOptions = new TrustOptionsImpl(trustManSupplier, "RSA");

    Assertions.assertArrayEquals(trustManSupplier.get(), trustOptions.getTrustManagerFactory(null).getTrustManagers());
  }

  @Test
  public void testCloneReturnsSuppliedTrustManagers() throws Exception {
    Supplier<TrustManager[]> trustManSupplier = () -> new TrustManager[]{
            new DummyTestTrustManager("foobar1"),
            new DummyTestTrustManager("foobar2"),
            new DummyTestTrustManager("foobar3"),
    };

    TrustOptions trustOptions = new TrustOptionsImpl(trustManSupplier, "RSA").clone();

    Assertions.assertArrayEquals(trustManSupplier.get(), trustOptions.getTrustManagerFactory(null).getTrustManagers());
  }

  @Test
  public void testDetectIncorrectOrder() throws Exception {
    Supplier<TrustManager[]> trustManSupplier = () -> new TrustManager[]{
            new DummyTestTrustManager("foobar1"),
            new DummyTestTrustManager("foobar2"),
            new DummyTestTrustManager("foobar3"),
    };

    TrustManager[] shuffledManagers = new TrustManager[]{
            new DummyTestTrustManager("foobar2"),
            new DummyTestTrustManager("foobar1"),
            new DummyTestTrustManager("foobar3"),
    };

    TrustOptions trustOptions = new TrustOptionsImpl(trustManSupplier, "RSA").clone();

    Assertions.assertFalse(Arrays.equals(shuffledManagers, trustOptions.getTrustManagerFactory(null).getTrustManagers()));
  }

  @Test
  public void testNoErrorsOnInit() throws Exception {
    Supplier<TrustManager[]> trustManSupplier = () -> new TrustManager[]{
            new DummyTestTrustManager("foobar1"),
            new DummyTestTrustManager("foobar2"),
            new DummyTestTrustManager("foobar3"),
    };

    TrustOptions trustOptions = new TrustOptionsImpl(trustManSupplier, "RSA").clone();

    TrustManagerFactory trustManagerFactory = trustOptions.getTrustManagerFactory(null);

    trustManagerFactory.init((KeyStore) null);
    trustManagerFactory.init((ManagerFactoryParameters) null);
  }

  private class DummyTestTrustManager implements TrustManager {
    private final String test;

    DummyTestTrustManager(String test) {
      this.test = test;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DummyTestTrustManager that = (DummyTestTrustManager) o;
      return test.equals(that.test);
    }

    @Override
    public int hashCode() {
      return test != null ? test.hashCode() : 0;
    }
  }
}