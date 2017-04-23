package com.domhauton.membrane.distributed.block.manifest;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
class DistributedShard {
  private final String shardId;
  private Priority priority;
  private Set<String> storedByPeers;

  DistributedShard(String shardId, Priority priority) {
    this.shardId = shardId;
    this.priority = priority;
    storedByPeers = new HashSet<>();
  }

  void upgradePriority(Priority priority) {
    if (priority.getValue() > this.priority.getValue()) {
      this.priority = priority;
    }
  }

  int requiredPeers() {
    return priority.getRequiredCopies() - storedByPeers.size();
  }

  void addPeer(String peer) {
    storedByPeers.add(peer);
  }

  void removePeer(String peer) {
    storedByPeers.remove(peer);
  }

  boolean isStoredBy(String peer) {
    return storedByPeers.contains(peer);
  }

  String getShardId() {
    return shardId;
  }

  boolean isPartiallyDeployed() {
    return !storedByPeers.isEmpty() && requiredPeers() > 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DistributedShard that = (DistributedShard) o;

    return (shardId != null ? shardId.equals(that.shardId) : that.shardId == null) &&
        priority == that.priority && (storedByPeers != null ? storedByPeers.equals(that.storedByPeers) : that.storedByPeers == null);
  }

  @Override
  public int hashCode() {
    int result = shardId != null ? shardId.hashCode() : 0;
    result = 31 * result + (priority != null ? priority.hashCode() : 0);
    result = 31 * result + (storedByPeers != null ? storedByPeers.hashCode() : 0);
    return result;
  }
}
