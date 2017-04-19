package com.domhauton.membrane.distributed.block.gen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
public class BlockProcessor {
  private static final Logger LOGGER = LogManager.getLogger();
  private static final int SALT_LENGTH = 256;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final byte[] salt;
  private final Map<String, LocalShardData> localShardDataList;

  public BlockProcessor() {
    salt = generateRandomSalt();
    localShardDataList = new HashMap<>();
  }

  public BlockProcessor(byte[] data, String key) throws BlockException {
    BlockContainer blockContainer = BlockUtils.bytes2Block(data, key);
    salt = blockContainer.getSalt();
    localShardDataList = blockContainer.getLocalShardDataList()
            .stream()
            .collect(Collectors.toMap(LocalShardData::getLocalId, Function.identity()));
  }

  /**
   * Add a shard to the block processor
   *
   * @param hash      the hash of the data being added. Must be correct. No double-check
   * @param shardData the data of the shard to add
   * @return Size of the added shard after compression in bytes
   */
  public int addLocalShard(String hash, byte[] shardData) {
    try {
      byte[] compressedData = BlockUtils.compress(shardData);
      LocalShardData localShardData = new LocalShardData(hash, true, compressedData);
      localShardDataList.put(hash, localShardData);
      return compressedData.length;
    } catch (BlockException e) {
      LOGGER.trace("Unable to effectively compress shard [{}]. Adding uncompressed. {}", hash, e.getMessage());
      LocalShardData localShardData = new LocalShardData(hash, false, shardData);
      localShardDataList.put(hash, localShardData);
      return shardData.length;
    }
  }

  public Map<String, byte[]> getShardMap() {
    return localShardDataList.values()
        .stream()
        .map((LocalShardData x) -> {
          try {
            return new AbstractMap.SimpleEntry<>(x.getLocalId(), x.isCompressed() ? BlockUtils.decompress(x.getShardData()) : x.getShardData());
          } catch (BlockException e) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
  }

  /**
   * Retrieve shard data from block.
   *
   * @param hash hash of shard to retrieve
   * @return data to retrieve
   * @throws NoSuchElementException If block with given hash not inside.
   */
  byte[] getBlock(String hash) throws NoSuchElementException {
    LocalShardData localShardData = localShardDataList.get(hash);
    if (localShardData == null) {
      throw new NoSuchElementException("Shard with local hash: [" + hash + "] not found.");
    } else {
      try {
        return localShardData.isCompressed() ? BlockUtils.decompress(localShardData.getShardData()) : localShardData.getShardData();
      } catch (BlockException e) {
        throw new NoSuchElementException("Shard with local hash: [" + hash + "] found, but unable to decompress.");
      }
    }
  }

  /**
   * Convert the given block to bytes for transmission
   *
   * @return The block in byte form
   * @throws BlockException if unable to convert to bytes.
   */
  public byte[] toEncryptedBytes(String key) throws BlockException {
    BlockContainer blockContainer = new BlockContainer(salt, new ArrayList<>(localShardDataList.values()));
    return BlockUtils.block2Bytes(blockContainer, key);
  }

  private static byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    SECURE_RANDOM.nextBytes(salt);
    return salt;
  }
}
