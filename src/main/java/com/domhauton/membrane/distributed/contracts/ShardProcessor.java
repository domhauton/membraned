package com.domhauton.membrane.distributed.contracts;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 06/03/17.
 */
class ShardProcessor {
  private final String remoteId;

  private byte[] remoteShardData;

  ShardProcessor(byte[] remoteShardData) {
    this.remoteId = ContractUtils.getHash(remoteShardData);
  }

  synchronized byte[] getRemoteShardData() {
    return remoteShardData;
  }

  synchronized StorageContract getContract(String peer, DateTime end) {
    List<ShardConfirmation> shardConfirmationList = IntStream.range(0, DateTimeConstants.HOURS_PER_WEEK).boxed()
            .map(x -> ContractUtils.generateRandomSalt())
            .map(randBytes -> new ShardConfirmation(randBytes, ContractUtils.getHash(randBytes, remoteShardData)))
            .collect(Collectors.toList());

    return new StorageContract(peer, end, remoteId, shardConfirmationList);

  }
}
