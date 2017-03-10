package com.domhauton.membrane.distributed.evidence;

import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class ShardEvidence {
  private final DateTime end;
  private final DateTime start;

  // Proof of Existence Info
  private final String remoteShardId;
  private final List<ShardSaltHash> shardSaltHashList;

  ShardEvidence(DateTime start, DateTime end, String remoteShardId, List<ShardSaltHash> shardSaltHashList) {
    this.start = start;
    this.end = end;
    this.remoteShardId = remoteShardId;
    this.shardSaltHashList = shardSaltHashList;
  }

  String getRemoteShardId() {
    return remoteShardId;
  }

  public ShardSaltHash getShardConfirmation(DateTime dateTime) throws NoSuchElementException {
    int hoursFromStart = Hours.hoursBetween(start, dateTime).getHours();
    if (hoursFromStart < 0 || hoursFromStart >= shardSaltHashList.size()) {
      throw new NoSuchElementException("There is no shard confirmation for time " + dateTime.toString());
    } else {
      return shardSaltHashList.get(hoursFromStart);
    }
  }
}
