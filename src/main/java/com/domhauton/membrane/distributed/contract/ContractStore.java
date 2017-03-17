package com.domhauton.membrane.distributed.contract;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Dominic Hauton on 11/03/17.
 */
public class ContractStore {
  private final ConcurrentHashMap<String, StorageContract> contractList;

  public ContractStore() {
    contractList = new ConcurrentHashMap<>();
  }

  private StorageContract getStorageContract(String peerId) {
    return contractList.computeIfAbsent(peerId, x -> new StorageContract());
  }

  public void addMyBlockId(String peerId, String blockId) throws ContractStoreException {
    getStorageContract(peerId).addMyBlockId(blockId);
  }

  public void addPeerBlockId(String peerId, String blockId) throws ContractStoreException {
    getStorageContract(peerId).addPeerBlockId(blockId);
  }

  public void removeMyBlockId(String peerId, String blockId) throws ContractStoreException {
    getStorageContract(peerId).removeMyBlockId(blockId);
  }

  public Optional<String> removePeerBlockId(String peerId, String blockId) throws ContractStoreException {
    return getStorageContract(peerId).removePeerBlockId(blockId);
  }

  public int getMyBlockSpace(String peerId) throws ContractStoreException {
    return Math.max(0, getStorageContract(peerId).getRemainingMyBlockSpace());
  }

  public Set<String> getCurrentPeers() {
    return ImmutableSet.copyOf(contractList.keySet());
  }

  public Set<String> getMyBlockIds(String peerId) {
    return getStorageContract(peerId).getMyBlockIds();
  }

  public Set<String> getPeerBlockIds(String peerId) {
    return getStorageContract(peerId).getPeerBlockIds();
  }
}
