package com.domhauton.membrane.distributed.contracts;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class StorageContract {
  private final String peer;
  private final DateTime end;

  // Proof of Existence Info
  private final String remoteShardId;
  private final List<ShardConfirmation> shardConfirmationList;

  StorageContract(String peer, DateTime end, String remoteShardId, List<ShardConfirmation> shardConfirmationList) {
    this.peer = peer;
    this.end = end;
    this.remoteShardId = remoteShardId;
    this.shardConfirmationList = shardConfirmationList;
  }

  public String getPeer() {
    return peer;
  }

  public DateTime getEnd() {
    return end;
  }

  public String getRemoteShardId() {
    return remoteShardId;
  }

  public List<ShardConfirmation> getShardConfirmationList() {
    return shardConfirmationList;
  }
}
