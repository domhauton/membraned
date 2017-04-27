package com.domhauton.membrane.distributed.block.gen;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by Dominic Hauton on 09/03/17.
 */
public class BlockUtilsTest {
  private final static int RANDOM_SHARD_LEN = 1024;
  private final static Random RANDOM = new Random();
  final static byte[] LOREM_IPSUM_BYTES = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.".getBytes();

  @Test
  void snappyCompressDecompressTest() throws Exception {
    byte[] bytes = generateRandomShard();
    byte[] decompressedBytes;
    try {
      byte[] compressedBytes = BlockUtils.compress(bytes, BlockUtils.CompressionAlgo.SNAPPY);
      decompressedBytes = BlockUtils.decompress(compressedBytes, bytes.length, BlockUtils.CompressionAlgo.SNAPPY);
    } catch (BlockException e) {
      decompressedBytes = Arrays.copyOf(bytes, bytes.length);
    }
    Assertions.assertArrayEquals(bytes, decompressedBytes);
  }

  @Test
  void snappyUsefulCompressionTest() throws Exception {
    byte[] compressedBytes = BlockUtils.compress(LOREM_IPSUM_BYTES, BlockUtils.CompressionAlgo.SNAPPY);
    byte[] decompressedBytes = BlockUtils.decompress(compressedBytes, LOREM_IPSUM_BYTES.length, BlockUtils.CompressionAlgo.SNAPPY);
    Assertions.assertTrue(LOREM_IPSUM_BYTES.length > compressedBytes.length);
    Assertions.assertArrayEquals(LOREM_IPSUM_BYTES, decompressedBytes);
  }

  @Test
  void lz4CompressDecompressTest() throws Exception {
    byte[] bytes = generateRandomShard();
    byte[] decompressedBytes;
    try {
      byte[] compressedBytes = BlockUtils.compress(bytes, BlockUtils.CompressionAlgo.LZ4_FAST);
      decompressedBytes = BlockUtils.decompress(compressedBytes, bytes.length, BlockUtils.CompressionAlgo.LZ4_FAST);
    } catch (BlockException e) {
      decompressedBytes = Arrays.copyOf(bytes, bytes.length);
    }
    Assertions.assertArrayEquals(bytes, decompressedBytes);
  }

  @Test
  void lz4UsefulCompressionTest() throws Exception {
    byte[] compressedBytes = BlockUtils.compress(LOREM_IPSUM_BYTES, BlockUtils.CompressionAlgo.LZ4_FAST);
    byte[] decompressedBytes = BlockUtils.decompress(compressedBytes, LOREM_IPSUM_BYTES.length, BlockUtils.CompressionAlgo.LZ4_FAST);
    Assertions.assertTrue(LOREM_IPSUM_BYTES.length > compressedBytes.length);
    Assertions.assertArrayEquals(LOREM_IPSUM_BYTES, decompressedBytes);
  }

  @Test
  void encryptDecryptTest() throws Exception {
    byte[] bytes = generateRandomShard();

    String key = "thisISAKey";
    byte[] encryptedShard = BlockUtils.encrypt(bytes, key);
    byte[] decryptedShard = BlockUtils.decrypt(encryptedShard, key);
    Assertions.assertArrayEquals(bytes, decryptedShard);
  }

  @Test
  void encryptDecryptInvalidKeyTest() throws Exception {
    byte[] bytes = generateRandomShard();

    String key = "thisISAKey";
    String badKey = "thisISABadKey";
    byte[] encryptedShard = BlockUtils.encrypt(bytes, key);
    Assertions.assertThrows(BlockException.class, () -> BlockUtils.decrypt(encryptedShard, badKey));
  }

  @Test
  void encryptDecryptInvalidDataTest() throws Exception {
    byte[] bytes = generateRandomShard();

    String key = "thisISAKey";
    Assertions.assertThrows(BlockException.class, () -> BlockUtils.decrypt(bytes, key));
  }

  public static byte[] generateRandomShard() {
    byte[] newBytes = new byte[RANDOM_SHARD_LEN];
    RANDOM.nextBytes(newBytes);
    return newBytes;
  }

  @Test
  void calcBestShardsTest() {
    String[] shardList = new String[]{"shard1", "shard2", "shard3"};
    int[] sizeList = new int[]{3, 4, 5};
    Assertions.assertEquals(ImmutableSet.of("shard1", "shard2"), BlockUtils.calculateBestShards(shardList, sizeList, 7));
    Assertions.assertEquals(ImmutableSet.of("shard1", "shard3"), BlockUtils.calculateBestShards(shardList, sizeList, 8));
    Assertions.assertEquals(ImmutableSet.of("shard2", "shard3"), BlockUtils.calculateBestShards(shardList, sizeList, 9));
    Assertions.assertEquals(ImmutableSet.of("shard2", "shard3"), BlockUtils.calculateBestShards(shardList, sizeList, 10));
    Assertions.assertEquals(ImmutableSet.of("shard1", "shard2", "shard3"), BlockUtils.calculateBestShards(shardList, sizeList, 12));
  }
}