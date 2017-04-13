package com.domhauton.membrane.distributed.block.gen;

import com.google.common.hash.Hashing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 09/03/17.
 */
class BlockContainerBuilderTest {

  @Test
  void singleLocalDatumShardTest() throws Exception {
    BlockProcessor blockProcessor = new BlockProcessor();

    Map<String, byte[]> shardMap = IntStream.range(0, 1)
            .boxed()
            .map(x -> BlockUtilsTest.generateRandomShard())
            .collect(Collectors.toMap(x -> Hashing.md5().hashBytes(x).toString(), x -> x));

    shardMap.entrySet().forEach(x -> blockProcessor.addLocalShard(x.getKey(), x.getValue()));

    byte[] builtRemoteShard = blockProcessor.toBytes();

    BlockProcessor reproducedBlockProcessor = new BlockProcessor(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertArrayEquals(x.getValue(), reproducedBlockProcessor.getBlock(x.getKey())));
  }

  @Test
  void multipleLocalDatumShardTest() throws Exception {
    BlockProcessor blockProcessor = new BlockProcessor();

    Map<String, byte[]> shardMap = IntStream.range(0, 5)
            .boxed()
            .map(x -> BlockUtilsTest.generateRandomShard())
            .collect(Collectors.toMap(x -> Hashing.md5().hashBytes(x).toString(), x -> x));

    shardMap.put(Hashing.md5().hashBytes(BlockUtilsTest.LOREM_IPSUM_BYTES).toString(), BlockUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.entrySet().forEach(x -> blockProcessor.addLocalShard(x.getKey(), x.getValue()));

    byte[] builtRemoteShard = blockProcessor.toBytes();

    BlockProcessor reproducedBlockProcessor = new BlockProcessor(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertArrayEquals(x.getValue(), reproducedBlockProcessor.getBlock(x.getKey())));
  }

  @Test
  void corruptedCompressedDataTest() throws Exception {
    BlockProcessor blockProcessor = new BlockProcessor();

    Map<String, byte[]> shardMap = new HashMap<>();

    shardMap.put(Hashing.md5().hashBytes(BlockUtilsTest.LOREM_IPSUM_BYTES).toString(), BlockUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.entrySet().forEach(x -> blockProcessor.addLocalShard(x.getKey(), x.getValue()));

    byte[] builtRemoteShard = blockProcessor.toBytes();

    BlockProcessor reproducedBlockProcessor = new BlockProcessor(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertArrayEquals(x.getValue(), reproducedBlockProcessor.getBlock(x.getKey())));

    // Must add 64bits to be compliant with base64 encoding!

    builtRemoteShard = new String(builtRemoteShard).replace("shardData\":\"", "shardData\":\"corr").getBytes();

    BlockProcessor corruptedReproducedBlockProcessor = new BlockProcessor(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertThrows(NoSuchElementException.class, () -> corruptedReproducedBlockProcessor.getBlock(x.getKey())));
  }

  @Test
  void corruptedByteDataTest() throws Exception {
    BlockProcessor blockProcessor = new BlockProcessor();

    Map<String, byte[]> shardMap = new HashMap<>();

    shardMap.put(Hashing.md5().hashBytes(BlockUtilsTest.LOREM_IPSUM_BYTES).toString(), BlockUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.entrySet().forEach(x -> blockProcessor.addLocalShard(x.getKey(), x.getValue()));

    byte[] builtRemoteShard = blockProcessor.toBytes();

    BlockProcessor reproducedBlockProcessor = new BlockProcessor(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertArrayEquals(x.getValue(), reproducedBlockProcessor.getBlock(x.getKey())));

    // Must add 32bits to be break base64 encoding!

    byte[] corruptedBuiltRemoteShard = new String(builtRemoteShard).replace("shardData\":\"", "shardData\":\"co").getBytes();

    Assertions.assertThrows(BlockException.class, () -> new BlockProcessor(corruptedBuiltRemoteShard));

    byte[] corruptedBuiltRemoteShard2 = new String(builtRemoteShard).replace("shardData\":\"", "shardDaa\":\"").getBytes();

    Assertions.assertThrows(BlockException.class, () -> new BlockProcessor(corruptedBuiltRemoteShard2));
  }
}