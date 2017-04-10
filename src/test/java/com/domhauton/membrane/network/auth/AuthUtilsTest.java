package com.domhauton.membrane.network.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by dominic on 10/04/17.
 */
class AuthUtilsTest {

  @BeforeEach
  void setUp() {
    AuthUtils.addProvider();
  }

  @Test
  void verifyCorrectSignatures() throws AuthException {
    MembraneAuthInfo membraneAuthInfo = AuthUtils.generateAuthenticationInfo();
    String messageToSign = "THIS_is_a-varied-Message!";
    String fakeMessage = "THIS_is_a-varied-Message?";

    byte[] messageSignature = AuthUtils.signMessage(membraneAuthInfo.getPrivateKey(), messageToSign);

    Assertions.assertTrue(AuthUtils.verifySignedMessage(membraneAuthInfo.getX509Certificate(), messageToSign, messageSignature));
    Assertions.assertFalse(AuthUtils.verifySignedMessage(membraneAuthInfo.getX509Certificate(), fakeMessage, messageSignature));
    messageSignature[0] = (byte) ((messageSignature[0] + 1) % 64); // Try to corrupt it!
    Assertions.assertFalse(AuthUtils.verifySignedMessage(membraneAuthInfo.getX509Certificate(), messageToSign, messageSignature));
  }
}