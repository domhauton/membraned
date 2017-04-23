package com.domhauton.membrane.distributed.block.manifest;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class ShardPeerLookup {
  private ConcurrentHashMap<String, DistributedShard> distributedShardMap;

  public ShardPeerLookup() {
    this(new ConcurrentHashMap<>());
  }

  private ShardPeerLookup(ConcurrentHashMap<String, DistributedShard> distributedShardMap) {
    this.distributedShardMap = distributedShardMap;
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


  public void addStoragePeerForce(String md5Hash, String peer) {
    DistributedShard distributedShard = distributedShardMap.computeIfAbsent(md5Hash, shardId -> new DistributedShard(shardId, Priority.Normal));
    distributedShard.addPeer(peer);
  }

  /**
   * Returns the set of shards that need to be stored.
   *
   * @return Set of shards that could be stored by peers.
   */
  public Set<String> getShardsRequiringPeers() {
    return distributedShardMap.values().stream()
            .filter(distributedShard -> distributedShard.requiredPeers() > 0)
        .map(DistributedShard::getShardId)
        .collect(Collectors.toSet());
  }

  /**
   * Returns the set of shards that are partially deployed
   *
   * @return Set of shards that could be stored by peers.
   */
  public Set<String> partiallyDeployedShards() {
    return distributedShardMap.values().stream()
        .filter(DistributedShard::isPartiallyDeployed)
        .map(DistributedShard::getShardId)
        .collect(Collectors.toSet());
  }

  public Set<String> getFullyDeployedShards() {
    return distributedShardMap.values().stream()
        .filter(distributedShard -> distributedShard.requiredPeers() == 0)
        .map(DistributedShard::getShardId)
        .collect(Collectors.toSet());
  }

  /**
   * Returns the set of shards that need to be stored. That are not stored by this peer.
   *
   * @param peer Peer schedule from
   * @return Set of shards that could be stored by the peer.
   */
  public Set<String> getShardsRequiringPeers(String peer) {
    return distributedShardMap.values().stream()
            .filter(distributedShard -> distributedShard.requiredPeers() > 0)
            .filter(distributedShard -> !distributedShard.isStoredBy(peer))
        .map(DistributedShard::getShardId)
            .collect(Collectors.toSet());
  }
}
