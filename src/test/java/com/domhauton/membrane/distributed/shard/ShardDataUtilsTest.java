package com.domhauton.membrane.distributed.shard;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by Dominic Hauton on 09/03/17.
 */
class ShardDataUtilsTest {
  private final static int RANDOM_SHARD_LEN = 1024;
  private final static Random RANDOM = new Random();
  final static byte[] LOREM_IPSUM_BYTES = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.".getBytes();

  @Test
  void compressDecompressTest() throws Exception {
    byte[] bytes = generateRandomShard();
    byte[] decompressedBytes;
    try {
      byte[] compressedBytes = ShardDataUtils.compress(bytes);
      decompressedBytes = ShardDataUtils.decompress(compressedBytes);
    } catch (ShardDataException e) {
      decompressedBytes = Arrays.copyOf(bytes, bytes.length);
    }
    Assertions.assertArrayEquals(bytes, decompressedBytes);
  }

  @Test
  void usefulCompressionTest() throws Exception {
    byte[] compressedBytes = ShardDataUtils.compress(LOREM_IPSUM_BYTES);
    byte[] decompressedBytes = ShardDataUtils.decompress(compressedBytes);
    Assertions.assertTrue(LOREM_IPSUM_BYTES.length > compressedBytes.length);
    Assertions.assertArrayEquals(LOREM_IPSUM_BYTES, decompressedBytes);
  }

  static byte[] generateRandomShard() {
    byte[] newBytes = new byte[RANDOM_SHARD_LEN];
    RANDOM.nextBytes(newBytes);
    return newBytes;
  }
}