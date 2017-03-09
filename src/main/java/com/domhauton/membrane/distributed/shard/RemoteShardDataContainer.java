package com.domhauton.membrane.distributed.shard;

import java.util.ArrayList;

/**
 * Created by Dominic Hauton on 07/03/17.
 */
class RemoteShardDataContainer {
  private byte[] salt;
  private ArrayList<LocalShardData> localShardDataList;

  private RemoteShardDataContainer() {
  } // For Jackson ONLY

  RemoteShardDataContainer(byte[] salt, ArrayList<LocalShardData> localShardDataList) {
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
