package com.domhauton.membrane.distributed.evidence;

/**
 * Created by Dominic Hauton on 06/03/17.
 */
class ShardSaltHash {
  private final byte[] hashSalt;
  private final String hash;

  ShardSaltHash(byte[] hashSalt, String hash) {
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
