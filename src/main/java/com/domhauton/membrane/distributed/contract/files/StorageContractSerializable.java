package com.domhauton.membrane.distributed.contract.files;

import java.util.LinkedHashSet;

/**
 * Created by dominic on 13/04/17.
 */
public class StorageContractSerializable {
  private String peerId;
  private LinkedHashSet<String> myBlockIds;
  private LinkedHashSet<String> peerBlockIds;
  private int myBaseAllowedInequality = 0;
  private int peerBaseAllowedInequality = 0;

  private StorageContractSerializable() {
  } // Jackson ONLY

  public StorageContractSerializable(String peerId, LinkedHashSet<String> myBlockIds, LinkedHashSet<String> peerBlockIds, int myBaseAllowedInequality, int peerBaseAllowedInequality) {
    this.peerId = peerId;
    this.myBlockIds = myBlockIds;
    this.peerBlockIds = peerBlockIds;
    this.myBaseAllowedInequality = myBaseAllowedInequality;
    this.peerBaseAllowedInequality = peerBaseAllowedInequality;
  }

  public String getPeerId() {
    return peerId;
  }

  public LinkedHashSet<String> getMyBlockIds() {
    return myBlockIds;
  }

  public LinkedHashSet<String> getPeerBlockIds() {
    return peerBlockIds;
  }

  public int getMyBaseAllowedInequality() {
    return myBaseAllowedInequality;
  }

  public int getPeerBaseAllowedInequality() {
    return peerBaseAllowedInequality;
  }
}
