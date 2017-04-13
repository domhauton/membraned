package com.domhauton.membrane.distributed.block.ledger.file;

import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by dominic on 13/04/17.
 */
public class BlockInfoSerializable {
  private String blockId;
  private String assignedPeer;
  private Set<String> containedShards;
  private long creationTime;
  private List<SaltHashPairSerializable> saltHashPairList;

  private BlockInfoSerializable() {
  } // Jackson ONLY

  public BlockInfoSerializable(String blockId, String assignedPeer, Set<String> containedShards, DateTime creationTime, List<SaltHashPairSerializable> saltHashPairList) {
    this.blockId = blockId;
    this.assignedPeer = assignedPeer;
    this.containedShards = new HashSet<>(containedShards);
    this.creationTime = creationTime.getMillis();
    this.saltHashPairList = saltHashPairList;
  }

  public String getBlockId() {
    return blockId;
  }

  public String getAssignedPeer() {
    return assignedPeer;
  }

  public Set<String> getContainedShards() {
    return containedShards;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public List<SaltHashPairSerializable> getSaltHashPairList() {
    return saltHashPairList;
  }
}
