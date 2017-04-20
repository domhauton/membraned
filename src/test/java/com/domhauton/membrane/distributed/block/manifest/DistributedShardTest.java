package com.domhauton.membrane.distributed.block.manifest;

import com.domhauton.membrane.distributed.ContractManagerException;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Created by dominic on 12/04/17.
 */
class DistributedShardTest {

  @Test
  void marshallUnmashallTest() throws ContractManagerException {
    String md5Hash = "shouldBeAHash";
    Priority priority = Priority.Normal;
    DistributedShard distributedShard = new DistributedShard(md5Hash, priority);
    String marshalledValue = distributedShard.marshall();
    DistributedShard unmarshalled = DistributedShard.unmarshall(marshalledValue);
    Assertions.assertEquals(distributedShard, unmarshalled);
  }

  @Test
  void marshallUnmashallPeersTest() throws ContractManagerException {
    String md5Hash = "shouldBeAHash";
    Priority priority = Priority.Normal;
    Set<String> peers = ImmutableSet.of("peer1", "peer2");
    DistributedShard distributedShard = new DistributedShard(md5Hash, priority);
    String marshalledValue = distributedShard.marshall();
    DistributedShard unmarshalled = DistributedShard.unmarshall(marshalledValue);
    Assertions.assertEquals(distributedShard, unmarshalled);
  }

  @Test
  void marshallUnmashallCorruptTest() throws ContractManagerException {
    String md5Hash = "shouldBeAHash";
    Priority priority = Priority.Normal;
    Set<String> peers = ImmutableSet.of("peer1", "peer2");
    DistributedShard distributedShard = new DistributedShard(md5Hash, priority);
    String marshalledValue = distributedShard.marshall();
    String marshalledValueCorrupt = marshalledValue.replaceAll("Normal", "invalid");
    Assertions.assertThrows(ContractManagerException.class, () -> DistributedShard.unmarshall(marshalledValueCorrupt));
  }

  @Test
  void marshallUnmashallNoCommasTest() throws ContractManagerException {
    String md5Hash = "shouldBeAHash";
    Priority priority = Priority.Normal;
    Set<String> peers = ImmutableSet.of("peer1", "peer2");
    DistributedShard distributedShard = new DistributedShard(md5Hash, priority);
    String marshalledValue = distributedShard.marshall();
    String marshalledValueCorrupt = marshalledValue.replaceAll(",", ".");
    Assertions.assertThrows(ContractManagerException.class, () -> DistributedShard.unmarshall(marshalledValueCorrupt));
  }
}