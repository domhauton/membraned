package com.domhauton.membrane.distributed.shard;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
abstract class ShardDataUtils {
  private static ObjectMapper objectMapper = new ObjectMapper()
          .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NON_PRIVATE);

  static RemoteShardDataContainer bytes2RemoteShardData(byte[] message) throws ShardDataException {
    try {
      return objectMapper.readValue(message, RemoteShardDataContainer.class);
    } catch (IOException e) {
      throw new ShardDataException("Could not decode shard. Error: " + e.getMessage(), e);
    }
  }

  static byte[] remoteShardData2Bytes(RemoteShardDataContainer message) throws ShardDataException {
    try {
      return objectMapper.writeValueAsString(message).getBytes();
    } catch (JsonProcessingException e) { // Should never be spontaneously generated
      throw new ShardDataException("Could not encode shard. Error: " + e.getMessage(), e);
    }
  }

  static byte[] compress(byte[] data) throws ShardDataException {
    try {
      byte[] compressedData = Snappy.compress(data);
      if (compressedData.length >= data.length) {
        throw new IOException("Compression ineffective.");
      } else {
        return compressedData;
      }
    } catch (IOException e) {
      throw new ShardDataException("Compression stopped.", e);
    }
  }

  static byte[] decompress(byte[] compressed) throws ShardDataException {
    try {
      return Snappy.uncompress(compressed);
    } catch (IOException e) {
      throw new ShardDataException("Decompression not possible.", e);
    }
  }
}
