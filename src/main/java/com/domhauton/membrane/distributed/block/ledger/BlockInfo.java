package com.domhauton.membrane.distributed.block.ledger;

import java.util.Set;

/**
 * Created by dominic on 13/04/17.
 */
public class BlockInfo {
  private final String blockId;
  private final Evidence evidence;
  private final String assignedPeer;
  private final Set<String> containedShards;

  public BlockInfo(String blockId, Evidence evidence, String assignedPeer, Set<String> containedShards) {
    this.blockId = blockId;
    this.evidence = evidence;
    this.assignedPeer = assignedPeer;
    this.containedShards = containedShards;
  }

  public String getBlockId() {
    return blockId;
  }

  public Evidence getEvidence() {
    return evidence;
  }

  public String getAssignedPeer() {
    return assignedPeer;
  }

  public Set<String> getContainedShards() {
    return containedShards;
  }
}
