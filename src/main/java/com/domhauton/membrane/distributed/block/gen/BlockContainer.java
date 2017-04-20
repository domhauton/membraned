package com.domhauton.membrane.distributed.block.gen;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Dominic Hauton on 07/03/17.
 */
class BlockContainer {
  private byte[] salt;
  private ArrayList<LocalShardData> localShardDataList;
  private Set<String> fileHistory;

  private BlockContainer() {
  } // For Jackson ONLY

  BlockContainer(byte[] salt, ArrayList<LocalShardData> localShardDataList, Set<String> fileHistory) {
    this.salt = salt;
    this.localShardDataList = localShardDataList;
    this.fileHistory = fileHistory;
  }

  ArrayList<LocalShardData> getLocalShardDataList() {
    return localShardDataList;
  }

  public Set<String> getFileHistory() {
    return fileHistory;
  }

  byte[] getSalt() {
    return salt;
  }
}
