package com.domhauton.membrane.distributed.block.gen;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
class LocalShardData {
  private String localId;
  private String compressionAlgo;
  private int compressedLength;
  private byte[] shardData;

  private LocalShardData() {
  } // For Jackson ONLY

  public LocalShardData(String localId, String compressionAlgo, int compressedLength, byte[] shardData) {
    this.localId = localId;
    this.compressionAlgo = compressionAlgo;
    this.compressedLength = compressedLength;
    this.shardData = shardData;
  }

  public String getLocalId() {
    return localId;
  }

  public byte[] getShardData() {
    return shardData;
  }

  public int getCompressedLength() {
    return compressedLength;
  }

  public String getCompressionAlgo() {
    return compressionAlgo;
  }
}
