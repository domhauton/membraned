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
  private static final BlockUtils.CompressionAlgo COMPRESSION_ALGO = BlockUtils.CompressionAlgo.LZ4_FAST;

  private final byte[] salt;
  private final Map<String, LocalShardData> localShardDataList;
  private final Set<String> fileHistory;

  public BlockProcessor() {
    salt = generateRandomSalt();
    localShardDataList = new HashMap<>();
    fileHistory = new HashSet<>();
  }

  public BlockProcessor(byte[] data, String key) throws BlockException {
    BlockContainer blockContainer = BlockUtils.bytes2Block(data, key);
    salt = blockContainer.getSalt();
    localShardDataList = blockContainer.getLocalShardDataList()
            .stream()
            .collect(Collectors.toMap(LocalShardData::getLocalId, Function.identity()));
    fileHistory = blockContainer.getFileHistory();
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
      byte[] compressedData = BlockUtils.compress(shardData, COMPRESSION_ALGO);
      LocalShardData localShardData = new LocalShardData(hash, COMPRESSION_ALGO.name(), shardData.length, compressedData);
      localShardDataList.put(hash, localShardData);
      return compressedData.length;
    } catch (BlockException e) {
      LOGGER.trace("Unable to effectively compress shard [{}]. Adding uncompressed. {}", hash, e.getMessage());
      LocalShardData localShardData = new LocalShardData(hash, BlockUtils.CompressionAlgo.NONE.name(), shardData.length, shardData);
      localShardDataList.put(hash, localShardData);
      return shardData.length;
    }
  }

  public void addFileHistory(Set<String> history) {
    fileHistory.addAll(history);
  }

  public Set<String> getFileHistory() {
    return fileHistory;
  }

  public int getShardCount() {
    return localShardDataList.size();
  }

  public Map<String, byte[]> getShardMap() {
    return localShardDataList.values()
        .stream()
        .map((LocalShardData x) -> {
          try {
            return new AbstractMap.SimpleEntry<>(x.getLocalId(), BlockUtils.decompress(x.getShardData(), x.getCompressedLength(), x.getCompressionAlgo()));
          } catch (BlockException e) {
            LOGGER.error("Error retrieving block from block processor. {}", e.getMessage());
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
        return BlockUtils.decompress(localShardData.getShardData(), localShardData.getCompressedLength(), localShardData.getCompressionAlgo());
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
    BlockContainer blockContainer = new BlockContainer(salt, new ArrayList<>(localShardDataList.values()), fileHistory);
    return BlockUtils.block2Bytes(blockContainer, key);
  }

  private static byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    SECURE_RANDOM.nextBytes(salt);
    return salt;
  }
}
