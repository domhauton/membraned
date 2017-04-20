package com.domhauton.membrane.distributed.block.gen;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 09/03/17.
 */
class BlockContainerBuilderTest {

  private final static String ENCRYPTION_KEY = "ThisIs4N3NcrypTiOnKee";

  @Test
  void singleLocalDatumShardTest() throws Exception {
    BlockProcessor blockProcessor = new BlockProcessor();

    Map<String, byte[]> shardMap = IntStream.range(0, 1)
            .boxed()
            .map(x -> BlockUtilsTest.generateRandomShard())
            .collect(Collectors.toMap(x -> Hashing.md5().hashBytes(x).toString(), x -> x));

    shardMap.forEach(blockProcessor::addLocalShard);

    Set<String> fileHistory = ImmutableSet.of("hist info 1", "info 23");

    blockProcessor.addFileHistory(fileHistory);

    byte[] builtRemoteShard = blockProcessor.toEncryptedBytes(ENCRYPTION_KEY);

    BlockProcessor reproducedBlockProcessor = new BlockProcessor(builtRemoteShard, ENCRYPTION_KEY);

    Assertions.assertEquals(fileHistory, reproducedBlockProcessor.getFileHistory());

    shardMap.forEach((key, value) -> Assertions.assertArrayEquals(value, reproducedBlockProcessor.getBlock(key)));
  }

  @Test
  void multipleLocalDatumShardTest() throws Exception {
    BlockProcessor blockProcessor = new BlockProcessor();

    Map<String, byte[]> shardMap = IntStream.range(0, 5)
            .boxed()
            .map(x -> BlockUtilsTest.generateRandomShard())
            .collect(Collectors.toMap(x -> Hashing.md5().hashBytes(x).toString(), x -> x));

    shardMap.put(Hashing.md5().hashBytes(BlockUtilsTest.LOREM_IPSUM_BYTES).toString(), BlockUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.forEach(blockProcessor::addLocalShard);

    byte[] builtRemoteShard = blockProcessor.toEncryptedBytes(ENCRYPTION_KEY);

    BlockProcessor reproducedBlockProcessor = new BlockProcessor(builtRemoteShard, ENCRYPTION_KEY);

    shardMap.forEach((key, value) -> Assertions.assertArrayEquals(value, reproducedBlockProcessor.getBlock(key)));
  }

  @Test
  void corruptedCompressedDataTest() throws Exception {
    BlockProcessor blockProcessor = new BlockProcessor();

    Map<String, byte[]> shardMap = new HashMap<>();

    shardMap.put(Hashing.md5().hashBytes(BlockUtilsTest.LOREM_IPSUM_BYTES).toString(), BlockUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.forEach(blockProcessor::addLocalShard);

    byte[] builtRemoteShard = blockProcessor.toEncryptedBytes(ENCRYPTION_KEY);

    BlockProcessor reproducedBlockProcessor = new BlockProcessor(builtRemoteShard, ENCRYPTION_KEY);

    shardMap.forEach((key, value) -> Assertions.assertArrayEquals(value, reproducedBlockProcessor.getBlock(key)));

    // Must add 64bits to be compliant with base64 encoding!

    byte[] corruptedRemoteShard = new String(builtRemoteShard).replace("shardData\":\"", "shardData\":\"corr").getBytes();

    Assertions.assertThrows(BlockException.class, () -> new BlockProcessor(corruptedRemoteShard, ENCRYPTION_KEY));
  }

  @Test
  void corruptedByteDataTest() throws Exception {
    BlockProcessor blockProcessor = new BlockProcessor();

    Map<String, byte[]> shardMap = new HashMap<>();

    shardMap.put(Hashing.md5().hashBytes(BlockUtilsTest.LOREM_IPSUM_BYTES).toString(), BlockUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.forEach(blockProcessor::addLocalShard);

    byte[] builtRemoteShard = blockProcessor.toEncryptedBytes(ENCRYPTION_KEY);

    BlockProcessor reproducedBlockProcessor = new BlockProcessor(builtRemoteShard, ENCRYPTION_KEY);

    shardMap.forEach((key, value) -> Assertions.assertArrayEquals(value, reproducedBlockProcessor.getBlock(key)));

    // Must add 32bits to be break base64 encoding!

    byte[] corruptedBuiltRemoteShard = new String(builtRemoteShard).replace("shardData\":\"", "shardData\":\"co").getBytes();

    Assertions.assertThrows(BlockException.class, () -> new BlockProcessor(corruptedBuiltRemoteShard, ENCRYPTION_KEY));

    byte[] corruptedBuiltRemoteShard2 = new String(builtRemoteShard).replace("shardData\":\"", "shardDaa\":\"").getBytes();

    Assertions.assertThrows(BlockException.class, () -> new BlockProcessor(corruptedBuiltRemoteShard2, ENCRYPTION_KEY));
  }
}