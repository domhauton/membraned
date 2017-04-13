package com.domhauton.membrane.distributed.block.gen;

import java.util.ArrayList;

/**
 * Created by Dominic Hauton on 07/03/17.
 */
class BlockContainer {
  private byte[] salt;
  private ArrayList<LocalShardData> localShardDataList;

  private BlockContainer() {
  } // For Jackson ONLY

  BlockContainer(byte[] salt, ArrayList<LocalShardData> localShardDataList) {
    this.salt = salt;
    this.localShardDataList = localShardDataList;
  }

  ArrayList<LocalShardData> getLocalShardDataList() {
    return localShardDataList;
  }

  byte[] getSalt() {
    return salt;
  }
}
