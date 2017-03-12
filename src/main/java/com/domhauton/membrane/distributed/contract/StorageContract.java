package com.domhauton.membrane.distributed.contract;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Dominic Hauton on 11/03/17.
 */
class StorageContract {
  private Set<String> myBlockIds;
  private Set<String> peerBlockIds;

  StorageContract() {
    myBlockIds = new HashSet<>();
    peerBlockIds = new HashSet<>();
  }


  synchronized void addMyBlockId(String blockId) throws ContractStoreException {
    if (!myBlockIds.contains(blockId)) {
      if (getRemainingMyBlockSpace() > 0) {
        myBlockIds.add(blockId);
      } else {
        throw new ContractStoreException("Insufficient space for [" + blockId + "]");
      }
    }
  }

  synchronized void addPeerBlockId(String blockId) throws ContractStoreException {
    if (!peerBlockIds.contains(blockId)) {
      if (getRemainingPeerSpace() > 0) {
        peerBlockIds.add(blockId);
      } else {
        throw new ContractStoreException("Insufficient space for [" + blockId + "]");
      }
    }
  }

  synchronized void removeMyBlockId(String myBlockId) {
    myBlockIds.remove(myBlockId);
  }

  synchronized void removePeerBlockId(String peerBlockId) {
    peerBlockIds.remove(peerBlockId);
  }

  synchronized Set<String> getMyBlockIds() {
    return ImmutableSet.copyOf(myBlockIds);
  }

  synchronized Set<String> getPeerBlockIds() {
    return ImmutableSet.copyOf(peerBlockIds);
  }

  /**
   * Number of blocks the peer is holding for you, minus the blocks you are holding for them.
   *
   * @return +ve if more of your blocks.
   */
  private int getBlockInequality() {
    return myBlockIds.size() - peerBlockIds.size();
  }

  /**
   * Amount of inequality allowed
   *
   * @return 1 + 10% of current holdings
   */
  private int getBlockInequalityThreshold() {
    return 1 + (Math.min(myBlockIds.size(), peerBlockIds.size()) / 10);
  }

  private int getRemainingPeerSpace() {
    return getBlockInequalityThreshold() + getBlockInequality();
  }

  int getRemainingMyBlockSpace() {
    return Math.max(0, getBlockInequalityThreshold() - getBlockInequality());
  }

  double getStorageBalance() {
    double maxBlocks = Math.max(myBlockIds.size(), peerBlockIds.size());
    return maxBlocks > 0 ? (double) getBlockInequality() / maxBlocks : 0.0d;
  }
}
