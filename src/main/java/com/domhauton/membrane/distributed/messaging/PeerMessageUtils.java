package com.domhauton.membrane.distributed.messaging;

import com.domhauton.membrane.distributed.messaging.messages.PeerMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by dominic on 12/02/17.
 */
public abstract class PeerMessageUtils {
  private static ObjectMapper objectMapper = new ObjectMapper();

  public static PeerMessage bytes2Message(byte[] message) throws PeerMessageException {
    try {
      return objectMapper.readValue(message, PeerMessage.class);
    } catch (IOException e) {
      throw new PeerMessageException("Could not decode message. Error: " + e.getMessage());
    }
  }

  public static byte[] message2Bytes(PeerMessage message) throws PeerMessageException {
    try {
      return objectMapper.writeValueAsString(message).getBytes();
    } catch (JsonProcessingException e) {
      throw new PeerMessageException("Could not encode message. Error: " + e.getMessage());
    }
  }
}
