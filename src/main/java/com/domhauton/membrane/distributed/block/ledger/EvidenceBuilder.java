package com.domhauton.membrane.distributed.block.ledger;

import com.google.common.hash.Hashing;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 06/03/17.
 */
class EvidenceBuilder {
  private static final int SALT_LENGTH = 256;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private byte[] blockData;

  EvidenceBuilder(byte[] blockData) {
    this.blockData = blockData;
  }

  Evidence build(DateTime end) {
    DateTime start = DateTime.now();
    start = start.withTime(start.getHourOfDay(), 0, 0, 0); // Flatten to nearest hour
    int hoursBetween = Math.max(0, Hours.hoursBetween(start, end).getHours());
    List<SaltHashPair> saltHashPairList = IntStream.range(0, hoursBetween + 1).boxed()
        .map(x -> generateRandomSalt())
        .map(randSaltBytes -> new SaltHashPair(randSaltBytes, getHash(randSaltBytes, blockData)))
            .collect(Collectors.toList());
    return new Evidence(start, saltHashPairList);
  }

  private byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    SECURE_RANDOM.nextBytes(salt);
    return salt;
  }

  static String getHash(byte[] salt, byte[] blockData) {
    return Hashing.sha512()
        .newHasher(blockData.length + salt.length)
        .putBytes(salt)
        .putBytes(blockData)
        .hash()
        .toString();
  }
}
