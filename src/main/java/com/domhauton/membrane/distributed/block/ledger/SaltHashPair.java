package com.domhauton.membrane.distributed.block.ledger;

/**
 * Created by Dominic Hauton on 06/03/17.
 */
public class SaltHashPair {
  private final byte[] hashSalt;
  private final String hash;

  SaltHashPair(byte[] hashSalt, String hash) {
    this.hashSalt = hashSalt;
    this.hash = hash;
  }

  byte[] getHashSalt() {
    return hashSalt;
  }

  String getHash() {
    return hash;
  }
}
