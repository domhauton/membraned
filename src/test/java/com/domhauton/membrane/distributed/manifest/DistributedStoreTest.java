package com.domhauton.membrane.distributed.manifest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * Created by Dominic Hauton on 16/03/17.
 */
class DistributedStoreTest {

  @Test
  void addSingleShard() throws Exception {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    Set<String> distributedShards = distributedStore.undeployedShards();
    Assertions.assertEquals(1, distributedShards.size());
  }

  @Test
  void addTwoShards() throws Exception {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    distributedStore.addDistributedShard("shard2", Priority.Normal);
    Set<String> distributedShards = distributedStore.undeployedShards();
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

  @Test
  void distributedStoreMarshalling() {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    distributedStore.addDistributedShard("shard2", Priority.Normal);
    String marshalledStore = distributedStore.marshall();

    DistributedStore unmarshalledStore = DistributedStore.unmarshall(Arrays.asList(marshalledStore.split("\n")));
    Set<String> distributedShards = unmarshalledStore.undeployedShards();
    Assertions.assertEquals(2, distributedShards.size());
  }

  @Test
  void distributedStoreMarshallingCorrupt() {
    DistributedStore distributedStore = new DistributedStore();
    distributedStore.addDistributedShard("shard1", Priority.Normal);
    distributedStore.addDistributedShard("shard2", Priority.Normal);
    String marshalledStore = distributedStore.marshall();

    List<String> splitMarshalledStore = new ArrayList<>(Arrays.asList(marshalledStore.split("\n")));
    splitMarshalledStore.add(0, splitMarshalledStore.get(0).replaceAll("ormal", ""));
    splitMarshalledStore.remove(1);

    DistributedStore unmarshalledStore = DistributedStore.unmarshall(splitMarshalledStore);
    Set<String> distributedShards = unmarshalledStore.undeployedShards();
    Assertions.assertEquals(1, distributedShards.size());
  }
}