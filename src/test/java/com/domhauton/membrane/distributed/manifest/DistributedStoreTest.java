package com.domhauton.membrane.distributed.manifest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Created by Dominic Hauton on 16/03/17.
 */
class DistributedStoreTest {

  @Test
  void addSingleShard() throws Exception {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    Set<DistributedShard> distributedShards = distributedStore.undeployedShards();
    Assertions.assertEquals(1, distributedShards.size());
  }

  @Test
  void addTwoShards() throws Exception {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    distributedStore.addDistributedShard("shard2", Priority.Normal);
    Set<DistributedShard> distributedShards = distributedStore.undeployedShards();
    Assertions.assertEquals(2, distributedShards.size());
  }

  @Test
  void peerFilteringTest() throws Exception {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    distributedStore.addDistributedShard("shard2", Priority.Normal);
    distributedStore.addStoragePeer("shard1", "peer1");
    Assertions.assertEquals(1, distributedStore.undeployedShards("peer1").size());
    distributedStore.addStoragePeer("shard2", "peer1");
    Assertions.assertEquals(0, distributedStore.undeployedShards("peer1").size());
    distributedStore.removeStoragePeer("shard1", "peer1");
    Assertions.assertEquals(1, distributedStore.undeployedShards("peer1").size());
  }

  @Test
  void peerAddFailTest() throws Exception {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    Assertions.assertEquals(1, distributedStore.undeployedShards("peer1").size());
    distributedStore.addStoragePeer("shard1", "peer1");
    Assertions.assertEquals(0, distributedStore.undeployedShards("peer1").size());

    Assertions.assertThrows(NoSuchElementException.class, () -> distributedStore.addStoragePeer("shard2", "peer1"));
    Assertions.assertThrows(NoSuchElementException.class, () -> distributedStore.removeStoragePeer("shard2", "peer1"));
  }

  @Test
  void priorityBumpTest() throws Exception {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Lax);
    distributedStore.addStoragePeer("shard1", "peer1");
    distributedStore.addStoragePeer("shard1", "peer2");
    Assertions.assertEquals(0, distributedStore.undeployedShards().size());

    distributedStore.addDistributedShard("shard1", Priority.Normal);
    Assertions.assertEquals(1, distributedStore.undeployedShards().size());

    distributedStore.addStoragePeer("shard1", "peer3");
    Assertions.assertEquals(0, distributedStore.undeployedShards().size());

    distributedStore.addDistributedShard("shard1", Priority.Critical);
    Assertions.assertEquals(1, distributedStore.undeployedShards().size());

    distributedStore.addStoragePeer("shard1", "peer4");
    Assertions.assertEquals(0, distributedStore.undeployedShards().size());
  }
}