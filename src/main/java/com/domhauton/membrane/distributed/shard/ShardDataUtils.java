package com.domhauton.membrane.distributed.shard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
public abstract class ShardDataUtils {
  private static ObjectMapper objectMapper = new ObjectMapper();

  static RemoteShardData bytes2RemoteShardData(byte[] message) throws ShardDataException {
    try {
      return objectMapper.readValue(message, RemoteShardData.class);
    } catch (IOException e) {
      throw new ShardDataException("Could not decode shard. Error: " + e.getMessage());
    }
  }

  static byte[] remoteShardData2Bytes(RemoteShardData message) throws ShardDataException {
    try {
      return objectMapper.writeValueAsString(message).getBytes();
    } catch (JsonProcessingException e) {
      throw new ShardDataException("Could not encode shard. Error: " + e.getMessage());
    }
  }
}
