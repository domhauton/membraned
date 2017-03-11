package com.domhauton.membrane.distributed.evidence;

import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class BlockEvidence {
  private final DateTime start;

  // Proof of Existence Info
  private final String remoteShardId;
  private final List<BlockSaltHash> blockSaltHashList;

  BlockEvidence(DateTime start, String remoteShardId, List<BlockSaltHash> blockSaltHashList) {
    this.start = start;
    this.remoteShardId = remoteShardId;
    this.blockSaltHashList = blockSaltHashList;
  }

  String getBlockId() {
    return remoteShardId;
  }

  BlockSaltHash getBlockConfirmation(DateTime dateTime) throws NoSuchElementException {
    int hoursFromStart = Hours.hoursBetween(start, dateTime).getHours();
    if (hoursFromStart < 0 || hoursFromStart >= blockSaltHashList.size()) {
      throw new NoSuchElementException("There is no shard confirmation for time " + dateTime.toString());
    } else {
      return blockSaltHashList.get(hoursFromStart);
    }
  }
}
