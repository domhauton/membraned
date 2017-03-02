package com.domhauton.membrane.network.messaging;

import com.domhauton.membrane.network.messaging.messages.PeerMessage;
import com.domhauton.membrane.network.messaging.messages.PingMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by Dominic Hauton on 24/02/17.
 */
public class PeerMessageUtilsTest {
  private static final String TEST_SENDER = "user1";
  private static final String TEST_RECIPIENT = "user2";
  private static final String TEST_VERSION = "T1.0.0-nonprod.1";

  @Test
  void encodeDecodePingTest() throws Exception {
    PingMessage pingMessage = new PingMessage(TEST_SENDER, TEST_RECIPIENT, TEST_VERSION);
    byte[] encodedMessage = PeerMessageUtils.message2Bytes(pingMessage);
    System.out.println(new String(encodedMessage));
    PeerMessage decodedMessage = PeerMessageUtils.bytes2Message(encodedMessage);
    Assertions.assertEquals(pingMessage, decodedMessage);
  }
}