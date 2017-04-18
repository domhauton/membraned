package com.domhauton.membrane.distributed.contract;

import com.domhauton.membrane.distributed.contract.files.StorageContractSerializable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Dominic Hauton on 11/03/17.
 */
class StorageContract {
  private String peerId;
  private LinkedHashSet<String> myBlockIds;
  private LinkedHashSet<String> peerBlockIds;
  private int myBaseAllowedInequality = 0;
  private int peerBaseAllowedInequality = 0;

  StorageContract(String peerId) {
    this.peerId = peerId;
    myBlockIds = new LinkedHashSet<>();
    peerBlockIds = new LinkedHashSet<>();
  }

  StorageContract(String peerId, LinkedHashSet<String> myBlockIds, LinkedHashSet<String> peerBlockIds, int myBaseAllowedInequality, int peerBaseAllowedInequality) {
    this.peerId = peerId;
    this.myBlockIds = new LinkedHashSet<>(myBlockIds);
    this.peerBlockIds = new LinkedHashSet<>(peerBlockIds);
    this.myBaseAllowedInequality = myBaseAllowedInequality;
    this.peerBaseAllowedInequality = peerBaseAllowedInequality;
  }

  public String getPeerId() {
    return peerId;
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

  synchronized void addMyBlockIdForce(String blockId) {
    myBlockIds.add(blockId);
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
    if (getRemainingPeerSpace() < 0 && peerBlockIds.size() > 0) {
      peerBlockIds.remove(Iterables.get(peerBlockIds, 0));
    }
  }

  synchronized Optional<String> removePeerBlockId(String peerBlockId) {
    peerBlockIds.remove(peerBlockId);
    if (getRemainingMyBlockSpace() < 0 && myBlockIds.size() > 0) {
      String removedLocalBlock = Iterables.get(myBlockIds, 0);
      myBlockIds.remove(removedLocalBlock);
      return Optional.of(removedLocalBlock);
    } else {
      return Optional.empty();
    }
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
  private int getBlockInequalityThreshold(int base) {
    return base + (Math.min(myBlockIds.size(), peerBlockIds.size()) / 10);
  }

  private int getRemainingPeerSpace() {
    return getBlockInequalityThreshold(peerBaseAllowedInequality) + getBlockInequality();
  }

  int getRemainingMyBlockSpace() {
    return getBlockInequalityThreshold(myBaseAllowedInequality) - getBlockInequality();
  }

  void setMyBaseAllowedInequality(int myBaseAllowedInequality) {
    this.myBaseAllowedInequality = myBaseAllowedInequality;
  }

  void setPeerBaseAllowedInequality(int peerBaseAllowedInequality) {
    this.peerBaseAllowedInequality = peerBaseAllowedInequality;
  }

  int getPeerBaseAllowedInequality() {
    return peerBaseAllowedInequality;
  }

  StorageContractSerializable serialize() {
    return new StorageContractSerializable(peerId, myBlockIds, peerBlockIds, myBaseAllowedInequality, peerBaseAllowedInequality);
  }
}
