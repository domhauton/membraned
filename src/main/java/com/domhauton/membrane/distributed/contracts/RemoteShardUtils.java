package com.domhauton.membrane.distributed.contracts;

import com.google.common.hash.Hashing;

import java.security.SecureRandom;

/**
 * Created by Dominic Hauton on 07/03/17.
 */
abstract class RemoteShardUtils {
  private static final int SALT_LENGTH = 256;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  static byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    SECURE_RANDOM.nextBytes(salt);
    return salt;
  }

  static byte[] computeRemoteShardData(byte[] salt, byte[] inputShardData) {
    //FIXME Correctly wrap and compute local shard data.
    return inputShardData;
  }

  static String getHash(byte[] salt, byte[] remoteShardData) {
    return Hashing.sha512()
            .newHasher(remoteShardData.length + salt.length)
            .putBytes(salt)
            .putBytes(remoteShardData)
            .toString();
  }

  static String getHash(byte[] remoteShardData) {
    return Hashing.sha512()
            .hashBytes(remoteShardData)
            .toString();
  }
}
