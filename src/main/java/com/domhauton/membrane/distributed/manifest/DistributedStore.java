package com.domhauton.membrane.distributed.manifest;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class DistributedStore {
  private ConcurrentHashMap<String, DistributedShard> distributedShardMap;

  public DistributedStore() {
    this.distributedShardMap = new ConcurrentHashMap<>();
  }

  public void addDistributedShard(String md5Hash, Priority priority) {
    DistributedShard distributedShard = distributedShardMap
            .getOrDefault(md5Hash, new DistributedShard(md5Hash, priority));
    distributedShard.upgradePriority(priority);
    distributedShardMap.put(md5Hash, distributedShard);
  }

  private DistributedShard getShard(String md5Hash) throws NoSuchElementException {
    DistributedShard distributedShard = distributedShardMap.get(md5Hash);
    if (distributedShard != null) {
      return distributedShard;
    } else {
      throw new NoSuchElementException(md5Hash + " does not exist.");
    }
  }

  public void removeStoragePeer(String md5Hash, String peer) throws NoSuchElementException {
    getShard(md5Hash).removePeer(peer);
  }

  public void addStoragePeer(String md5Hash, String peer) throws NoSuchElementException {
    getShard(md5Hash).addPeer(peer);
  }

  /**
   * Returns the set of shards that need to be stored.
   *
   * @return Set of shards that could be stored by peers.
   */
  public Set<String> undeployedShards() {
    return distributedShardMap.values().stream()
            .filter(distributedShard -> distributedShard.requiredPeers() > 0)
            .map(DistributedShard::getMd5Hash)
            .collect(Collectors.toSet());
  }

  /**
   * Returns the set of shards that need to be stored. That are not stored by this peer.
   *
   * @param peer Peer schedule from
   * @return Set of shards that could be stored by the peer.
   */
  public Set<String> undeployedShards(String peer) {
    return distributedShardMap.values().stream()
            .filter(distributedShard -> distributedShard.requiredPeers() > 0)
            .filter(distributedShard -> !distributedShard.isStoredBy(peer))
            .map(DistributedShard::getMd5Hash)
            .collect(Collectors.toSet());
  }
}
