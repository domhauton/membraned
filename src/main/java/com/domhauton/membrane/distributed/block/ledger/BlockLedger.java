package com.domhauton.membrane.distributed.block.ledger;

import com.domhauton.membrane.distributed.block.ledger.file.BlockInfoCollection;
import com.domhauton.membrane.distributed.block.ledger.file.BlockInfoSerializable;
import com.domhauton.membrane.distributed.block.manifest.ShardPeerLookup;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class BlockLedger implements Runnable, Closeable {
  private final static String FILE_NAME = "blocks.yml";
  private static final int SALT_LENGTH = 256;
  private final static int PERSIST_UPDATE_RATE_SEC = 120;
  private static final int HMAC_SIZE = 512;

  private final Logger logger = LogManager.getLogger();
  private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
  private final SecureRandom secureRandom = new SecureRandom();
  private final ScheduledExecutorService executorService;

  private Map<String, BlockInfo> blockMap;
  private final Path fullPersistPath;

  public BlockLedger(Path basePath) throws BlockLedgerException {
    this.blockMap = new HashMap<>();
    executorService = Executors.newSingleThreadScheduledExecutor();

    if (!basePath.toFile().exists()) {
      try {
        Files.createDirectories(basePath);
      } catch (IOException e) {
        logger.error("Could not create non-existent base path");
        throw new BlockLedgerException("Could not create base path.", e);
      }
    }

    fullPersistPath = Paths.get(basePath.toString() + File.separator + FILE_NAME);

    List<BlockInfo> blockInfos = fullPersistPath.toFile().exists() ?
        readBlockInfos(fullPersistPath) : Collections.emptyList();

    blockInfos.forEach(x -> blockMap.put(x.getBlockId(), x));
  }

  public String addBlock(byte[] data, Set<String> containedShards, String assignedPeer, DateTime endDateTime) {
    String blockId = generateBlockId(data);
    DateTime startDateTime = DateTime.now().hourOfDay().roundFloorCopy();
    List<SaltHashPair> saltHashPairs = generateEvidencePairs(data, startDateTime, endDateTime);
    BlockInfo blockInfo = new BlockInfo(blockId, assignedPeer, containedShards, startDateTime, saltHashPairs);
    blockMap.put(blockId, blockInfo);
    return blockId;
  }

  public byte[] getBlockEvidenceSalt(String blockId, DateTime dateTime) throws BlockLedgerException {
    BlockInfo blockInfo = blockMap.get(blockId);
    if (blockInfo != null) {
      return blockInfo.getBlockConfirmation(dateTime).getHashSalt();
    } else {
      throw new BlockLedgerException("Block " + blockId + " does not exist.");
    }
  }

  public Set<String> getBlockShardIds(String blockId) throws BlockLedgerException {
    BlockInfo blockInfo = blockMap.get(blockId);
    if (blockInfo != null) {
      return blockInfo.getContainedShards();
    } else {
      throw new BlockLedgerException("Block " + blockId + " does not exist.");
    }
  }

  public boolean isBlockExpired(String blockId, DateTime dateTime) throws BlockLedgerException {
    BlockInfo blockInfo = blockMap.get(blockId);
    if (blockInfo != null) {
      try {
        blockInfo.getBlockConfirmation(dateTime);
        return blockInfo.isForceExpired();
      } catch (NoSuchElementException e) {
        return true;
      }
    } else {
      throw new BlockLedgerException("Block " + blockId + " does not exist.");
    }
  }

  public boolean confirmBlockHash(String blockId, DateTime dateTime, String testHash) throws BlockLedgerException {
    BlockInfo blockInfo = blockMap.get(blockId);
    if (blockInfo != null) {
      return blockInfo.getBlockConfirmation(dateTime).getHash().equals(testHash);
    } else {
      throw new BlockLedgerException("Block " + blockId + " does not exist.");
    }
  }

  public void expireAllUselessBlocks(Set<String> requiredShardIds) {
    blockMap.values()
        .stream()
        .filter(x -> Collections.disjoint(requiredShardIds, x.getContainedShards()))
        .forEach(BlockInfo::expire);
  }

  public boolean removeBlock(String blockId) {
    return blockMap.remove(blockId) != null;
  }

  public void removeAllExcept(Set<String> blockIds) {
    Set<String> removalSet = blockMap.keySet().stream()
        .filter(o -> !blockIds.contains(o))
        .collect(Collectors.toSet());
    removalSet.forEach(this::removeBlock);
  }

  public ShardPeerLookup generateShardPeerLookup() {
    ShardPeerLookup shardPeerLookup = new ShardPeerLookup();
    blockMap.values().stream()
        .filter(x -> !x.isForceExpired())
        .flatMap(x -> x.getContainedShards().stream().map(shardId -> new AbstractMap.SimpleEntry<>(shardId, x.getAssignedPeer())))
        .forEach(x -> shardPeerLookup.addStoragePeerForce(x.getKey(), x.getValue()));
    return shardPeerLookup;
  }

  public static String generateBlockId(byte[] blockData) {
    return Hashing.sha512()
        .hashBytes(blockData)
        .toString();
  }

  private List<SaltHashPair> generateEvidencePairs(byte[] blockData, DateTime start, DateTime end) {
    int hoursBetween = Math.max(0, Hours.hoursBetween(start, end).getHours());
    Set<Integer> missingHourSet = IntStream.range(1, hoursBetween)
        .filter(x -> secureRandom.nextFloat() > 0.5)
        .limit(hoursBetween / 2)
        .boxed()
        .collect(Collectors.toSet());
    Set<Integer> fullBlockSet = IntStream.range(1, hoursBetween)
        .filter(missingHourSet::contains)
        .filter(x -> secureRandom.nextFloat() > 0.99)
        .limit(hoursBetween / 100)
        .boxed()
        .collect(Collectors.toSet());
    return IntStream.range(0, hoursBetween + 1).boxed()
        .map(x -> {
          if (missingHourSet.contains(x)) {
            return new SaltHashPair(PROOF_TYPE.EMPTY.toString().getBytes(), "");
          } else if (fullBlockSet.contains(x)) {
            return new SaltHashPair(PROOF_TYPE.FULL.toString().getBytes(), "");
          } else {
            byte[] randomSalt = generateRandomSalt();
            return new SaltHashPair(randomSalt, getHMAC(randomSalt, blockData));
          }
        })
        .collect(Collectors.toList());
  }

  private byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    secureRandom.nextBytes(salt);
    return salt;
  }

  public static String getHMAC(byte[] salt, byte[] blockData) {
    HMac hmac = new HMac(new KeccakDigest(HMAC_SIZE));
    KeyParameter keyParameter = new KeyParameter(salt);
    hmac.init(keyParameter);
    hmac.update(blockData, 0, blockData.length);
    byte[] retHash = new byte[HMAC_SIZE];
    hmac.doFinal(retHash, 0);
    return new String(retHash);
  }

  private synchronized List<BlockInfo> readBlockInfos(Path path) throws BlockLedgerException {
    try {
      logger.info("Reading block info from file. [{}]", path);
      List<BlockInfoSerializable> blockInfos =
          objectMapper.readValue(path.toFile(), BlockInfoCollection.class).getBlockInfos();
      return blockInfos.stream()
          .map(x -> new BlockInfo(x.getBlockId(), x.getAssignedPeer(), x.getContainedShards(), x.getCreationTime(), x.getSaltHashPairList()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Reading block info from file failed. [{}]", path);
      logger.debug(e);
      throw new BlockLedgerException("Failed to read block info from file.", e);
    }
  }

  void writeBlockInfo() throws BlockLedgerException {
    writeBlockInfo(fullPersistPath);
  }

  private synchronized void writeBlockInfo(Path path) throws BlockLedgerException {
    List<BlockInfoSerializable> blockInfoSerializableList = blockMap.values().stream()
        .map(BlockInfo::serialize)
        .collect(Collectors.toList());
    BlockInfoCollection blockInfoCollection = new BlockInfoCollection(blockInfoSerializableList);
    try {
      logger.info("Storing {} block infos to file. [{}]", blockInfoSerializableList.size(), path);
      Path tmpPath = Paths.get(path.toString() + ".tmp");
      Files.deleteIfExists(tmpPath);
      objectMapper.writeValue(tmpPath.toFile(), blockInfoCollection);
      Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      logger.error("Failed to store block info to file. [{}] {}", path, e.getMessage());
      throw new BlockLedgerException("Failed to open file. ", e);
    }
  }

  @Override
  public void close() {
    try {
      writeBlockInfo(fullPersistPath);
    } catch (BlockLedgerException e) {
      logger.error("Failed to complete block info persist on Ledger lose. {}", e.getMessage());
    }
    executorService.shutdown();
  }

  @Override
  public void run() {
    executorService.scheduleAtFixedRate(() -> {
      try {
        writeBlockInfo(fullPersistPath);
      } catch (BlockLedgerException e) {
        logger.error("Failed to complete scheduled block info persist. {}", e.getMessage());
      }
    }, 0, PERSIST_UPDATE_RATE_SEC, TimeUnit.SECONDS);
  }

  public enum PROOF_TYPE {
    FULL, EMPTY
  }
}
