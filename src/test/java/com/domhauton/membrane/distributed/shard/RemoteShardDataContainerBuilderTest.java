package com.domhauton.membrane.distributed.shard;

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
class RemoteShardDataContainerBuilderTest {

  @Test
  void singleLocalDatumShardTest() throws Exception {
    RemoteShardData remoteShardData = new RemoteShardData();

    Map<String, byte[]> shardMap = IntStream.range(0, 1)
            .boxed()
            .map(x -> ShardDataUtilsTest.generateRandomShard())
            .collect(Collectors.toMap(x -> Hashing.md5().hashBytes(x).toString(), x -> x));

    shardMap.entrySet().forEach(x -> remoteShardData.addLocalShard(x.getKey(), x.getValue()));

    byte[] builtRemoteShard = remoteShardData.toBytes();

    RemoteShardData reproducedRemoteShardData = new RemoteShardData(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertArrayEquals(x.getValue(), reproducedRemoteShardData.getShard(x.getKey())));
  }

  @Test
  void multipleLocalDatumShardTest() throws Exception {
    RemoteShardData remoteShardData = new RemoteShardData();

    Map<String, byte[]> shardMap = IntStream.range(0, 5)
            .boxed()
            .map(x -> ShardDataUtilsTest.generateRandomShard())
            .collect(Collectors.toMap(x -> Hashing.md5().hashBytes(x).toString(), x -> x));

    shardMap.put(Hashing.md5().hashBytes(ShardDataUtilsTest.LOREM_IPSUM_BYTES).toString(), ShardDataUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.entrySet().forEach(x -> remoteShardData.addLocalShard(x.getKey(), x.getValue()));

    byte[] builtRemoteShard = remoteShardData.toBytes();

    RemoteShardData reproducedRemoteShardData = new RemoteShardData(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertArrayEquals(x.getValue(), reproducedRemoteShardData.getShard(x.getKey())));
  }

  @Test
  void corruptedCompressedDataTest() throws Exception {
    RemoteShardData remoteShardData = new RemoteShardData();

    Map<String, byte[]> shardMap = new HashMap<>();

    shardMap.put(Hashing.md5().hashBytes(ShardDataUtilsTest.LOREM_IPSUM_BYTES).toString(), ShardDataUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.entrySet().forEach(x -> remoteShardData.addLocalShard(x.getKey(), x.getValue()));

    byte[] builtRemoteShard = remoteShardData.toBytes();

    RemoteShardData reproducedRemoteShardData = new RemoteShardData(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertArrayEquals(x.getValue(), reproducedRemoteShardData.getShard(x.getKey())));

    // Must add 64bits to be compliant with base64 encoding!

    builtRemoteShard = new String(builtRemoteShard).replace("shardData\":\"", "shardData\":\"corr").getBytes();

    RemoteShardData corruptedReproducedRemoteShardData = new RemoteShardData(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertThrows(NoSuchElementException.class, () -> corruptedReproducedRemoteShardData.getShard(x.getKey())));
  }

  @Test
  void corruptedByteDataTest() throws Exception {
    RemoteShardData remoteShardData = new RemoteShardData();

    Map<String, byte[]> shardMap = new HashMap<>();

    shardMap.put(Hashing.md5().hashBytes(ShardDataUtilsTest.LOREM_IPSUM_BYTES).toString(), ShardDataUtilsTest.LOREM_IPSUM_BYTES);

    shardMap.entrySet().forEach(x -> remoteShardData.addLocalShard(x.getKey(), x.getValue()));

    byte[] builtRemoteShard = remoteShardData.toBytes();

    RemoteShardData reproducedRemoteShardData = new RemoteShardData(builtRemoteShard);

    shardMap.entrySet()
            .forEach(x -> Assertions.assertArrayEquals(x.getValue(), reproducedRemoteShardData.getShard(x.getKey())));

    // Must add 32bits to be break base64 encoding!

    byte[] corruptedBuiltRemoteShard = new String(builtRemoteShard).replace("shardData\":\"", "shardData\":\"co").getBytes();

    Assertions.assertThrows(ShardDataException.class, () -> new RemoteShardData(corruptedBuiltRemoteShard));

    byte[] corruptedBuiltRemoteShard2 = new String(builtRemoteShard).replace("shardData\":\"", "shardDaa\":\"").getBytes();

    Assertions.assertThrows(ShardDataException.class, () -> new RemoteShardData(corruptedBuiltRemoteShard2));
  }
}