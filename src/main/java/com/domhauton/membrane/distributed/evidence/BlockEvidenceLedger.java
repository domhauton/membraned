package com.domhauton.membrane.distributed.evidence;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class BlockEvidenceLedger {
  private Map<String, BlockEvidence> storageContractMap;

  public BlockEvidenceLedger() {
    this.storageContractMap = new HashMap<>();
  }

  String addNewContract(byte[] data, DateTime end) {
    BlockEvidence blockEvidence = new EvidenceBuilder(data).build(end);
    storageContractMap.put(blockEvidence.getBlockId(), blockEvidence);
    return blockEvidence.getBlockId();
  }

  byte[] getContractSalt(String remoteShardId, DateTime dateTime) throws NoSuchElementException {
    BlockEvidence blockEvidence = storageContractMap.get(remoteShardId);
    if (blockEvidence != null) {
      return blockEvidence.getBlockConfirmation(dateTime).getHashSalt();
    } else {
      throw new NoSuchElementException("Shard " + remoteShardId + " does not exist.");
    }
  }

  boolean confirmContractSaltHash(String remoteShardId, DateTime dateTime, String testHash) throws NoSuchElementException {
    BlockEvidence blockEvidence = storageContractMap.get(remoteShardId);
    if (blockEvidence != null) {
      return blockEvidence.getBlockConfirmation(dateTime).getHash().equals(testHash);
    } else {
      throw new NoSuchElementException("Shard " + remoteShardId + " does not exist.");
    }
  }

  boolean removeContract(String remoteShardId) {
    return storageContractMap.remove(remoteShardId) != null;
  }

  void removeAllExcept(Set<String> remoteShardIds) {
    Set<String> removalSet = storageContractMap.keySet().stream()
            .filter(o -> !remoteShardIds.contains(o))
            .collect(Collectors.toSet());
    removalSet.forEach(this::removeContract);
  }
}
