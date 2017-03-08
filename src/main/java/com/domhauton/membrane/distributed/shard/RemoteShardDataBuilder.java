package com.domhauton.membrane.distributed.shard;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
public class RemoteShardDataBuilder {
  private static final int SALT_LENGTH = 256;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final byte[] salt;
  private final List<LocalShardData> localShardDataList;

  public RemoteShardDataBuilder() {
    salt = generateRandomSalt();
    localShardDataList = new ArrayList<>();
  }

  public void addLocalShard(String hash, byte[] hashData) {
    LocalShardData localShardData = new LocalShardData(hash, hashData);
    localShardDataList.add(localShardData);
  }

  public byte[] build() throws ShardDataException {
    RemoteShardData remoteShardData = new RemoteShardData(salt, localShardDataList);
    return ShardDataUtils.remoteShardData2Bytes(remoteShardData);
  }

  public static RemoteShardData build(byte[] data) throws ShardDataException {
    return ShardDataUtils.bytes2RemoteShardData(data);
  }

  private static byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    SECURE_RANDOM.nextBytes(salt);
    return salt;
  }
}
