package com.domhauton.membrane.distributed.contract;

import com.domhauton.membrane.distributed.contract.files.StorageContractCollection;
import com.domhauton.membrane.distributed.contract.files.StorageContractSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 11/03/17.
 */
public class ContractStore implements Runnable, Closeable {
  private final static String FILE_NAME = "contracts.yml";
  private final static int PERSIST_UPDATE_RATE_SEC = 120;

  private final Logger logger = LogManager.getLogger();
  private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
  private final ConcurrentHashMap<String, StorageContract> contractList;
  private final ScheduledExecutorService executorService;

  private final Path fullPersistPath;

  public ContractStore(Path basePath) throws ContractStoreException {
    contractList = new ConcurrentHashMap<>();
    executorService = Executors.newSingleThreadScheduledExecutor();

    if (!basePath.toFile().exists()) {
      try {
        Files.createDirectories(basePath);
      } catch (IOException e) {
        logger.error("Could not create non-existent base path");
        throw new ContractStoreException("Could not create base path.", e);
      }
    }

    fullPersistPath = Paths.get(basePath.toString() + File.separator + FILE_NAME);

    List<StorageContract> contracts = fullPersistPath.toFile().exists() ?
        readContractInfo(fullPersistPath) : Collections.emptyList();

    contracts.forEach(x -> contractList.put(x.getPeerId(), x));
  }

  private StorageContract getStorageContract(String peerId) {
    return contractList.computeIfAbsent(peerId, x -> new StorageContract(peerId));
  }

  public void addMyBlockId(String peerId, String blockId) throws ContractStoreException {
    getStorageContract(peerId).addMyBlockId(blockId);
  }

  public void addMyBlockIdForce(String peerId, String blockId) {
    getStorageContract(peerId).addMyBlockIdForce(blockId);
  }

  public void addPeerBlockId(String peerId, String blockId) throws ContractStoreException {
    getStorageContract(peerId).addPeerBlockId(blockId);
  }

  public void removeMyBlockId(String peerId, String blockId) {
    getStorageContract(peerId).removeMyBlockId(blockId);
  }

  public Optional<String> removePeerBlockId(String peerId, String blockId) throws ContractStoreException {
    return getStorageContract(peerId).removePeerBlockId(blockId);
  }

  public int getMyBlockSpace(String peerId) throws ContractStoreException {
    return Math.max(0, getStorageContract(peerId).getRemainingMyBlockSpace());
  }

  public void setPeerAllowedInequality(String peerId, int value) {
    getStorageContract(peerId).setPeerBaseAllowedInequality(value);
  }

  public void setMyAllowedInequality(String peerId, int value) {
    getStorageContract(peerId).setMyBaseAllowedInequality(value);
  }

  public Set<String> getCurrentPeers() {
    return contractList.entrySet().stream()
        .filter(entry -> entry.getValue().getPeerBaseAllowedInequality() > 0 || entry.getValue().getMyBaseAllowedInequality() > 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  public void removeUselessPeers() {
    Set<String> currentPeers = getCurrentPeers();
    Set<String> uselessPeers = contractList.entrySet().stream().map(Map.Entry::getKey)
        .filter(peerId -> !currentPeers.contains(peerId))
        .collect(Collectors.toSet());
    uselessPeers.forEach(contractList::remove);
  }

  public Set<String> getMyBlockIds() {
    return contractList.values().stream()
        .flatMap(storageContract -> storageContract.getMyBlockIds().stream())
        .collect(Collectors.toSet());
  }

  public Set<String> getMyBlockIds(String peerId) {
    return getStorageContract(peerId).getMyBlockIds();
  }

  public int getMyBlockCount(String peerId) {
    return getStorageContract(peerId).getMyBlockCount();
  }

  public Set<String> getPeerBlockIds(String peerId) {
    return getStorageContract(peerId).getPeerBlockIds();
  }

  public Set<String> getPeerBlockIds() {
    return contractList.values().stream()
        .flatMap(storageContract -> storageContract.getPeerBlockIds().stream())
        .collect(Collectors.toSet());
  }

  public int getPeerAllowedInequality(String peerId) {
    return getStorageContract(peerId).getPeerBaseAllowedInequality();
  }

  private synchronized List<StorageContract> readContractInfo(Path path) throws ContractStoreException {
    try {
      logger.info("Reading block info from file. [{}]", path);
      List<StorageContractSerializable> contracts =
          objectMapper.readValue(path.toFile(), StorageContractCollection.class).getContracts();
      return contracts.stream()
          .map(x -> new StorageContract(x.getPeerId(), x.getMyBlockIds(), x.getPeerBlockIds(), x.getMyBaseAllowedInequality(), x.getPeerBaseAllowedInequality()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Reading contracts from file failed. [{}]", path);
      logger.debug(e);
      throw new ContractStoreException("Failed to read contracts from file. " + e.getMessage());
    }
  }

  private void writeContracts() throws ContractStoreException {
    writeContracts(fullPersistPath);
  }

  private synchronized void writeContracts(Path path) throws ContractStoreException {
    List<StorageContractSerializable> blockInfoSerializableList = contractList.values().stream()
        .map(StorageContract::serialize)
        .collect(Collectors.toList());
    StorageContractCollection blockInfoCollection = new StorageContractCollection(blockInfoSerializableList);
    try {
      logger.info("Storing {} contracts to file. [{}]", blockInfoSerializableList.size(), path);
      Path tmpPath = Paths.get(path.toString() + ".tmp");
      Files.deleteIfExists(tmpPath);
      objectMapper.writeValue(tmpPath.toFile(), blockInfoCollection);
      Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      logger.error("Failed to store contracts to file. [{}] {}", path, e.getMessage());
      throw new ContractStoreException("Failed to open file. ", e);
    }
  }

  @Override
  public void close() {
    try {
      writeContracts();
    } catch (ContractStoreException e) {
      logger.error("Failed to complete contract persist on contract store close. {}", e.getMessage());
    }
    executorService.shutdown();
  }

  @Override
  public void run() {
    executorService.scheduleAtFixedRate(() -> {
      try {
        writeContracts(fullPersistPath);
      } catch (ContractStoreException e) {
        logger.error("Failed to complete scheduled contract persist. {}", e.getMessage());
      }
    }, 0, PERSIST_UPDATE_RATE_SEC, TimeUnit.SECONDS);
  }
}
