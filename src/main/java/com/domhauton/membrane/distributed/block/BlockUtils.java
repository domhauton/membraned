package com.domhauton.membrane.distributed.block;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
abstract class BlockUtils {
  private static ObjectMapper objectMapper = new ObjectMapper()
          .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NON_PRIVATE);

  static BlockContainer bytes2RemoteShardData(byte[] message) throws BlockException {
    try {
      return objectMapper.readValue(message, BlockContainer.class);
    } catch (IOException e) {
      throw new BlockException("Could not decode shard. Error: " + e.getMessage(), e);
    }
  }

  static byte[] remoteShardData2Bytes(BlockContainer message) throws BlockException {
    try {
      return objectMapper.writeValueAsString(message).getBytes();
    } catch (JsonProcessingException e) { // Should never be spontaneously generated
      throw new BlockException("Could not encode shard. Error: " + e.getMessage(), e);
    }
  }

  static byte[] compress(byte[] data) throws BlockException {
    try {
      byte[] compressedData = Snappy.compress(data);
      if (compressedData.length >= data.length) {
        throw new IOException("Compression ineffective.");
      } else {
        return compressedData;
      }
    } catch (IOException e) {
      throw new BlockException("Compression stopped.", e);
    }
  }

  static byte[] decompress(byte[] compressed) throws BlockException {
    try {
      return Snappy.uncompress(compressed);
    } catch (IOException e) {
      throw new BlockException("Decompression not possible.", e);
    }
  }
}
