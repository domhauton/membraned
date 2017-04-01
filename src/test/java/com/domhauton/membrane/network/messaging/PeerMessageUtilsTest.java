package com.domhauton.membrane.network.messaging;

import com.domhauton.membrane.network.messaging.messages.PeerMessage;
import com.domhauton.membrane.network.messaging.messages.PingMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by Dominic Hauton on 24/02/17.
 */
public class PeerMessageUtilsTest {

  @Test
  void encodeDecodePingTest() throws Exception {
    PingMessage pingMessage = new PingMessage();
    byte[] encodedMessage = PeerMessageUtils.message2Bytes(pingMessage);
    System.out.println(new String(encodedMessage));
    PeerMessage decodedMessage = PeerMessageUtils.bytes2Message(encodedMessage);
    Assertions.assertEquals(pingMessage, decodedMessage);
  }
}