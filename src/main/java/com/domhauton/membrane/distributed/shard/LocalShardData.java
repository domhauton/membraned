package com.domhauton.membrane.distributed.shard;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
class LocalShardData {
  private String localId;
  private byte[] shardData;

  private LocalShardData() {
  } // For Jackson ONLY

  public LocalShardData(String localId, byte[] shardData) {
    this.localId = localId;
    this.shardData = shardData;
  }

  public String getLocalId() {
    return localId;
  }

  public byte[] getShardData() {
    return shardData;
  }
}
