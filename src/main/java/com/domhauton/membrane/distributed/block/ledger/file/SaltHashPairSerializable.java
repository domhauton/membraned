package com.domhauton.membrane.distributed.block.ledger.file;

/**
 * Created by dominic on 13/04/17.
 */
public class SaltHashPairSerializable {
  private byte[] hashSalt;
  private String hash;

  private SaltHashPairSerializable() {
  } // Jackson ONLY

  public SaltHashPairSerializable(String hash, byte[] hashSalt) {
    this.hashSalt = hashSalt;
    this.hash = hash;
  }

  public byte[] getHashSalt() {
    return hashSalt;
  }

  public String getHash() {
    return hash;
  }
}
