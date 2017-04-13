package com.domhauton.membrane.distributed.block.ledger;

import com.google.common.hash.Hashing;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class BlockLedger {
  private Map<String, BlockInfo> blockMap;

  public BlockLedger() {
    this.blockMap = new HashMap<>();
  }

  String addBlock(byte[] data, Set<String> containedShards, String assignedPeer, DateTime end) {
    String blockId = generateBlockId(data);
    EvidenceBuilder evidenceBuilder = new EvidenceBuilder(data);
    BlockInfo blockInfo = new BlockInfo(blockId, evidenceBuilder.build(end), assignedPeer, containedShards);
    blockMap.put(blockId, blockInfo);
    return blockId;
  }

  byte[] getBlockEvidenceSalt(String blockId, DateTime dateTime) throws NoSuchElementException {
    BlockInfo blockInfo = blockMap.get(blockId);
    if (blockInfo != null) {
      return blockInfo.getEvidence().getBlockConfirmation(dateTime).getHashSalt();
    } else {
      throw new NoSuchElementException("Block " + blockId + " does not exist.");
    }
  }

  boolean confirmBlockHash(String blockId, DateTime dateTime, String testHash) throws NoSuchElementException {
    BlockInfo blockInfo = blockMap.get(blockId);
    if (blockInfo != null) {
      return blockInfo.getEvidence().getBlockConfirmation(dateTime).getHash().equals(testHash);
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
}
