package com.domhauton.membrane.distributed.block.manifest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Created by Dominic Hauton on 16/03/17.
 */
class ShardPeerLookupTest {

  @Test
  void addSingleShard() throws Exception {
    ShardPeerLookup shardPeerLookup = new ShardPeerLookup();
    shardPeerLookup.addDistributedShard("shard1", Priority.Normal);
    shardPeerLookup.addDistributedShard("shard1", Priority.Normal);
    Set<String> distributedShards = shardPeerLookup.getShardsRequiringPeers();
    Assertions.assertEquals(1, distributedShards.size());
  }

  @Test
  void addTwoShards() throws Exception {
    ShardPeerLookup shardPeerLookup = new ShardPeerLookup();
    shardPeerLookup.addDistributedShard("shard1", Priority.Normal);
    shardPeerLookup.addDistributedShard("shard2", Priority.Normal);
    Set<String> distributedShards = shardPeerLookup.getShardsRequiringPeers();
    Assertions.assertEquals(2, distributedShards.size());
  }

  @Test
  void peerFilteringTest() throws Exception {
    ShardPeerLookup shardPeerLookup = new ShardPeerLookup();
    shardPeerLookup.addDistributedShard("shard1", Priority.Normal);
    shardPeerLookup.addDistributedShard("shard2", Priority.Normal);
    shardPeerLookup.addStoragePeer("shard1", "peer1");
    Assertions.assertEquals(1, shardPeerLookup.getShardsRequiringPeers("peer1").size());
    shardPeerLookup.addStoragePeer("shard2", "peer1");
    Assertions.assertEquals(0, shardPeerLookup.getShardsRequiringPeers("peer1").size());
    shardPeerLookup.removeStoragePeer("shard1", "peer1");
    Assertions.assertEquals(1, shardPeerLookup.getShardsRequiringPeers("peer1").size());
  }

  @Test
  void peerAddFailTest() throws Exception {
    ShardPeerLookup shardPeerLookup = new ShardPeerLookup();
    shardPeerLookup.addDistributedShard("shard1", Priority.Normal);
    Assertions.assertEquals(1, shardPeerLookup.getShardsRequiringPeers("peer1").size());
    shardPeerLookup.addStoragePeer("shard1", "peer1");
    Assertions.assertEquals(0, shardPeerLookup.getShardsRequiringPeers("peer1").size());

    Assertions.assertThrows(NoSuchElementException.class, () -> shardPeerLookup.addStoragePeer("shard2", "peer1"));
    Assertions.assertThrows(NoSuchElementException.class, () -> shardPeerLookup.removeStoragePeer("shard2", "peer1"));
  }

  @Test
  void priorityBumpTest() throws Exception {
    ShardPeerLookup shardPeerLookup = new ShardPeerLookup();
    shardPeerLookup.addDistributedShard("shard1", Priority.Normal);
    shardPeerLookup.addStoragePeer("shard1", "peer1");
    shardPeerLookup.addStoragePeer("shard1", "peer2");
    Assertions.assertEquals(1, shardPeerLookup.getShardsRequiringPeers().size());

    shardPeerLookup.addStoragePeer("shard1", "peer3");
    Assertions.assertEquals(0, shardPeerLookup.getShardsRequiringPeers().size());

    shardPeerLookup.addDistributedShard("shard1", Priority.Critical);
    Assertions.assertEquals(1, shardPeerLookup.getShardsRequiringPeers().size());

    shardPeerLookup.addStoragePeer("shard1", "peer4");
    shardPeerLookup.addStoragePeer("shard1", "peer5");
    shardPeerLookup.addStoragePeer("shard1", "peer6");
    Assertions.assertEquals(0, shardPeerLookup.getShardsRequiringPeers().size());
  }
}