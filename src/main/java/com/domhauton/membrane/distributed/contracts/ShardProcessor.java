package com.domhauton.membrane.distributed.contracts;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Dominic Hauton on 06/03/17.
 */
class ShardProcessor {


  private final String localShardId;
  private final byte[] shardSalt;
  private final String remoteId;

  private SecureRandom secureRandom;

  private byte[] remoteShardData;

  ShardProcessor(String localShardId, byte[] localShardData) {
    secureRandom = new SecureRandom();
    this.shardSalt = RemoteShardUtils.generateRandomSalt();
    this.localShardId = localShardId;
    this.remoteShardData = RemoteShardUtils.computeRemoteShardData(localShardData, shardSalt);
    this.remoteId = RemoteShardUtils.getHash(remoteShardData);
  }

  synchronized byte[] getRemoteShardData() {
    return remoteShardData;
  }

  synchronized StorageContract getContract(String peer, DateTime end) {
    List<ShardConfirmation> shardConfirmationList = IntStream.range(0, DateTimeConstants.HOURS_PER_WEEK).boxed()
            .map(x -> RemoteShardUtils.generateRandomSalt())
            .map(randBytes -> new ShardConfirmation(randBytes, RemoteShardUtils.getHash(randBytes, remoteShardData)))
            .collect(Collectors.toList());

    return new StorageContract(peer, end, localShardId, shardSalt, remoteId, shardConfirmationList);

  }
}
