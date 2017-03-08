package com.domhauton.membrane.distributed.shard;

import java.util.List;

/**
 * Created by Dominic Hauton on 07/03/17.
 */
public class RemoteShardData {
  private byte[] salt;
  private List<LocalShardData> localShardDatas;

  private RemoteShardData() {
  } // For Jackson ONLY

  RemoteShardData(byte[] salt, List<LocalShardData> localShardDatas) {
    this.salt = salt;
    this.localShardDatas = localShardDatas;
  }

  public List<LocalShardData> getLocalShardDatas() {
    return localShardDatas;
  }

  public byte[] getSalt() {
    return salt;
  }
}
