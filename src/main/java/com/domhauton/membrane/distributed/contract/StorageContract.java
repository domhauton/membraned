package com.domhauton.membrane.distributed.contract;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by Dominic Hauton on 11/03/17.
 */
public class StorageContract {
  private final String peerId;
  private Set<String> myBlockIds;
  private Set<String> peerBlockIds;

  public StorageContract(String peerId) {
    this.peerId = peerId;
  }

  public String getPeerId() {
    return peerId;
  }

  public synchronized StorageContract addMyBlockId(String myBlockId) {
    myBlockIds.add(myBlockId);
    return this;
  }

  public synchronized StorageContract addPeerBlockId(String peerBlockId) {
    peerBlockIds.add(peerBlockId);
    return this;
  }

  public synchronized Set<String> getMyBlockIds() {
    return ImmutableSet.copyOf(myBlockIds);
  }

  public synchronized Set<String> getPeerBlockIds() {
    return ImmutableSet.copyOf(peerBlockIds);
  }

  /**
   * Number of blocks the peer is holding for you, minus the blocks you are holding for them.
   *
   * @return +ve if more of your blocks.
   */
  int getBlockIdInequality() {
    return myBlockIds.size() - peerBlockIds.size();
  }

  /**
   * Amount of inequality allowed
   *
   * @return 1 + 10% of current holdings
   */
  int getBlockInequalityThreshold() {
    return 1 + (Math.min(myBlockIds.size(), peerBlockIds.size()) / 10);
  }
}
