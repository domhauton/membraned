package com.domhauton.membrane.distributed.shard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
class RemoteShardData {
  private static final Logger LOGGER = LogManager.getLogger();
  private static final int SALT_LENGTH = 256;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final byte[] salt;
  private final Map<String, LocalShardData> localShardDataList;

  RemoteShardData() {
    salt = generateRandomSalt();
    localShardDataList = new HashMap<>();
  }

  RemoteShardData(byte[] data) throws ShardDataException {
    RemoteShardDataContainer remoteShardDataContainer = ShardDataUtils.bytes2RemoteShardData(data);
    salt = remoteShardDataContainer.getSalt();
    localShardDataList = remoteShardDataContainer.getLocalShardDataList()
            .stream()
            .collect(Collectors.toMap(LocalShardData::getLocalId, Function.identity()));
  }

  int addLocalShard(String hash, byte[] shardData) {
    try {
      byte[] compressedData = ShardDataUtils.compress(shardData);
      LocalShardData localShardData = new LocalShardData(hash, true, compressedData);
      localShardDataList.put(hash, localShardData);
      return compressedData.length;
    } catch (ShardDataException e) {
      LOGGER.trace("Unable to effectively compress shard [{}]. Adding uncompressed. {}", hash, e.getMessage());
      LocalShardData localShardData = new LocalShardData(hash, false, shardData);
      localShardDataList.put(hash, localShardData);
      return shardData.length;
    }
  }

  byte[] getShard(String hash) throws NoSuchElementException {
    LocalShardData localShardData = localShardDataList.get(hash);
    if (localShardData == null) {
      throw new NoSuchElementException("Shard with local hash: [" + hash + "] not found.");
    } else {
      try {
        return localShardData.isCompressed() ? ShardDataUtils.decompress(localShardData.getShardData()) : localShardData.getShardData();
      } catch (ShardDataException e) {
        throw new NoSuchElementException("Shard with local hash: [" + hash + "] found, but unable to decompress.");
      }
    }
  }

  byte[] toBytes() throws ShardDataException {
    RemoteShardDataContainer remoteShardDataContainer = new RemoteShardDataContainer(salt, new ArrayList<>(localShardDataList.values()));
    return ShardDataUtils.remoteShardData2Bytes(remoteShardDataContainer);
  }

  private static byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    SECURE_RANDOM.nextBytes(salt);
    return salt;
  }
}
