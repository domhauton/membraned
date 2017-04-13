package com.domhauton.membrane.distributed.block.ledger.file;

import com.domhauton.membrane.distributed.block.ledger.SaltHashPair;
import org.joda.time.DateTime;

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
  private List<SaltHashPair> saltHashPairList;

  private BlockInfoSerializable() {
  } // Jackson ONLY

  public BlockInfoSerializable(String blockId, String assignedPeer, Set<String> containedShards, DateTime creationTime, List<SaltHashPair> saltHashPairList) {
    this.blockId = blockId;
    this.assignedPeer = assignedPeer;
    this.containedShards = containedShards;
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

  public List<SaltHashPair> getSaltHashPairList() {
    return saltHashPairList;
  }
}
