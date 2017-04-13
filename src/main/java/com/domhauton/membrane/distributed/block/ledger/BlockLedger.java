package com.domhauton.membrane.distributed.block.ledger;

import com.domhauton.membrane.distributed.block.ledger.file.BlockInfoCollection;
import com.domhauton.membrane.distributed.block.ledger.file.BlockInfoSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

  String addBlock(byte[] data, Set<String> containedShards, String assignedPeer, DateTime endDateTime) {
    String blockId = generateBlockId(data);
    DateTime startDateTime = DateTime.now().hourOfDay().roundFloorCopy();
    List<SaltHashPair> saltHashPairs = generateEvidencePairs(data, startDateTime, endDateTime);
    BlockInfo blockInfo = new BlockInfo(blockId, assignedPeer, containedShards, startDateTime, saltHashPairs);
    blockMap.put(blockId, blockInfo);
    return blockId;
  }

  byte[] getBlockEvidenceSalt(String blockId, DateTime dateTime) throws NoSuchElementException {
    BlockInfo blockInfo = blockMap.get(blockId);
    if (blockInfo != null) {
      return blockInfo.getBlockConfirmation(dateTime).getHashSalt();
    } else {
      throw new NoSuchElementException("Block " + blockId + " does not exist.");
    }
  }

  boolean confirmBlockHash(String blockId, DateTime dateTime, String testHash) throws NoSuchElementException {
    BlockInfo blockInfo = blockMap.get(blockId);
    if (blockInfo != null) {
      return blockInfo.getBlockConfirmation(dateTime).getHash().equals(testHash);
    } else {
      throw new NoSuchElementException("Block " + blockId + " does not exist.");
    }
  }

  boolean removeBlock(String blockId) {
    return blockMap.remove(blockId) != null;
  }

  void removeAllExcept(Set<String> blockIds) {
    Set<String> removalSet = blockMap.keySet().stream()
        .filter(o -> !blockIds.contains(o))
        .collect(Collectors.toSet());
    removalSet.forEach(this::removeBlock);
  }

  private String generateBlockId(byte[] blockData) {
    return Hashing.sha512()
        .hashBytes(blockData)
        .toString();
  }

  private List<SaltHashPair> generateEvidencePairs(byte[] blockData, DateTime start, DateTime end) {
    int hoursBetween = Math.max(0, Hours.hoursBetween(start, end).getHours());
    return IntStream.range(0, hoursBetween + 1).boxed()
        .map(x -> generateRandomSalt())
        .map(randSaltBytes -> new SaltHashPair(randSaltBytes, getHash(randSaltBytes, blockData)))
        .collect(Collectors.toList());
  }

  private byte[] generateRandomSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    secureRandom.nextBytes(salt);
    return salt;
  }

  String getHash(byte[] salt, byte[] blockData) {
    return Hashing.sha512()
        .newHasher(blockData.length + salt.length)
        .putBytes(salt)
        .putBytes(blockData)
        .hash()
        .toString();
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
      throw new BlockLedgerException("Failed to read block info from file. " + e.getMessage());
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
}
