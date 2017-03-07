package com.domhauton.membrane.distributed.contracts;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class StorageContract {
  private final String peer;
  private final DateTime end;

  // Shard Generation Info
  private final String localShardId;
  private final byte[] shardSalt;

  // Proof of Existence Info
  private final String remoteShardId;
  private final List<ShardConfirmation> shardConfirmationList;

  StorageContract(String peer, DateTime end, String localShardId, byte[] shardSalt, String remoteShardId, List<ShardConfirmation> shardConfirmationList) {
    this.peer = peer;
    this.end = end;
    this.localShardId = localShardId;
    this.shardSalt = shardSalt;
    this.remoteShardId = remoteShardId;
    this.shardConfirmationList = shardConfirmationList;
  }

  public String getPeer() {
    return peer;
  }

  public DateTime getEnd() {
    return end;
  }

  public String getLocalShardId() {
    return localShardId;
  }

  public byte[] getShardSalt() {
    return shardSalt;
  }

  public String getRemoteShardId() {
    return remoteShardId;
  }

  public List<ShardConfirmation> getShardConfirmationList() {
    return shardConfirmationList;
  }
}
