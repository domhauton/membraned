package com.domhauton.membrane.distributed.block;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
class LocalShardData {
  private String localId;
  private boolean compressed;
  private byte[] shardData;

  private LocalShardData() {
  } // For Jackson ONLY

  LocalShardData(String localId, boolean compressed, byte[] shardData) {
    this.localId = localId;
    this.compressed = compressed;
    this.shardData = shardData;
  }

  public String getLocalId() {
    return localId;
  }

  public byte[] getShardData() {
    return shardData;
  }

  public boolean isCompressed() {
    return compressed;
  }
}
