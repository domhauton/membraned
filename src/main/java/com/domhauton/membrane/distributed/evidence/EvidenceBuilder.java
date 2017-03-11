package com.domhauton.membrane.distributed.evidence;

import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 06/03/17.
 */
class EvidenceBuilder {
  private final String remoteId;

  private byte[] remoteShardData;

  EvidenceBuilder(byte[] blockData) {
    this.remoteId = BlockEvidenceUtils.getHash(blockData);
    this.remoteShardData = blockData;
  }


  BlockEvidence build(DateTime end) {
    DateTime start = DateTime.now();
    start = start.withTime(start.getHourOfDay(), 0, 0, 0); // Flatten to nearest hour
    int hoursBetween = Math.max(0, Hours.hoursBetween(start, end).getHours());
    List<BlockSaltHash> blockSaltHashList = IntStream.range(0, hoursBetween + 1).boxed()
            .map(x -> BlockEvidenceUtils.generateRandomSalt())
            .map(randSaltBytes -> new BlockSaltHash(randSaltBytes, BlockEvidenceUtils.getHash(randSaltBytes, remoteShardData)))
            .collect(Collectors.toList());
    return new BlockEvidence(start, remoteId, blockSaltHashList);
  }
}
