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
public class ShardEvidenceLedger {
  private Map<String, ShardEvidence> storageContractMap;

  public ShardEvidenceLedger() {
    this.storageContractMap = new HashMap<>();
  }

  String addNewContract(byte[] data, DateTime end) {
    ShardEvidence shardEvidence = new EvidenceBuilder(data).build(end);
    storageContractMap.put(shardEvidence.getRemoteShardId(), shardEvidence);
    return shardEvidence.getRemoteShardId();
  }

  byte[] getContractSalt(String remoteShardId, DateTime dateTime) throws NoSuchElementException {
    ShardEvidence shardEvidence = storageContractMap.get(remoteShardId);
    if (shardEvidence != null) {
      return shardEvidence.getShardConfirmation(dateTime).getHashSalt();
    } else {
      throw new NoSuchElementException("Shard " + remoteShardId + " does not exist.");
    }
  }

  boolean confirmContractSaltHash(String remoteShardId, DateTime dateTime, String testHash) throws NoSuchElementException {
    ShardEvidence shardEvidence = storageContractMap.get(remoteShardId);
    if (shardEvidence != null) {
      return shardEvidence.getShardConfirmation(dateTime).getHash().equals(testHash);
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
