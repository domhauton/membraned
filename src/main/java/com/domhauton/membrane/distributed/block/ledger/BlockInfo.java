package com.domhauton.membrane.distributed.block.ledger;

import com.domhauton.membrane.distributed.block.ledger.file.BlockInfoSerializable;
import com.domhauton.membrane.distributed.block.ledger.file.SaltHashPairSerializable;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 13/04/17.
 */
class BlockInfo {
  private final String blockId;
  private final String assignedPeer;
  private final Set<String> containedShards;
  private final DateTime evidenceStartTime;
  private final List<SaltHashPair> saltHashPairList;
  private boolean forceExpired = false;

  BlockInfo(String blockId, String assignedPeer, Set<String> containedShards, DateTime evidenceStartTime, List<SaltHashPair> saltHashPairList) {
    this.blockId = blockId;
    this.assignedPeer = assignedPeer;
    this.containedShards = containedShards;
    this.evidenceStartTime = evidenceStartTime;
    this.saltHashPairList = ImmutableList.copyOf(saltHashPairList);
  }

  BlockInfo(String blockId, String assignedPeer, Set<String> containedShards, long evidenceStartTimeMillis, List<SaltHashPairSerializable> saltHashPairList) {
    this.blockId = blockId;
    this.assignedPeer = assignedPeer;
    this.containedShards = new HashSet<>(containedShards);
    this.evidenceStartTime = new DateTime(Math.max(0, evidenceStartTimeMillis));
    this.saltHashPairList = saltHashPairList.stream()
        .map(x -> new SaltHashPair(x.getHashSalt(), x.getHash()))
        .collect(Collectors.toList());
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

  BlockInfoSerializable serialize() {
    List<SaltHashPairSerializable> saltHashPairSerialized = saltHashPairList.stream()
        .map(x -> new SaltHashPairSerializable(x.getHash(), x.getHashSalt()))
        .collect(Collectors.toList());
    return new BlockInfoSerializable(blockId, assignedPeer, containedShards, evidenceStartTime, saltHashPairSerialized);
  }

  SaltHashPair getBlockConfirmation(DateTime dateTime) throws BlockLedgerException {
    int hoursFromStart = Hours.hoursBetween(evidenceStartTime, dateTime).getHours();
    if (hoursFromStart < 0 || hoursFromStart >= saltHashPairList.size()) {
      throw new BlockLedgerException("There is no block confirmation for time " + dateTime.toString());
    } else {
      return saltHashPairList.get(hoursFromStart);
    }
  }

  void expire() {
    forceExpired = true;
  }

  public boolean isForceExpired() {
    return forceExpired;
  }
}
