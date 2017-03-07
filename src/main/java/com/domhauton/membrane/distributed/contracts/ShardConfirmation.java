package com.domhauton.membrane.distributed.contracts;

/**
 * Created by Dominic Hauton on 06/03/17.
 */
class ShardConfirmation {
  private final byte[] hashSalt;
  private final String hash;

  ShardConfirmation(byte[] hashSalt, String hash) {
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
