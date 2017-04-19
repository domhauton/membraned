package com.domhauton.membrane.shard;

import java.util.Set;

/**
 * Created by dominic on 31/01/17.
 */
public interface ShardStorage {
  /**
   * Store the data given give.
   *
   * @param shardId The md5Hash of the data given
   * @param data    The data to store.
   */
  void storeShard(String shardId, byte[] data) throws ShardStorageException;

  /**
   * Retrieves a shard from storage and checks consistency
   *
   * @param shardId md5Hash of requested shard
   * @return data requested.
   * @throws ShardStorageException If cannot access shard, shard does not exist or file corrupt.
   */
  byte[] retrieveShard(String shardId) throws ShardStorageException;

  /**
   * Retrieves a shard from storage and checks consistency
   *
   * @param shardId md5Hash of requested shard
   * @return true if shard is stored.
   */
  boolean hasShard(String shardId);

  /**
   * Retrieves the size of a share from storage
   *
   * @param shardId md5Hash of requested shard
   * @return size of requested shard in bytes. 0L if unknown.
   * @throws ShardStorageException If shard does not exist.
   */
  long getShardSize(String shardId) throws ShardStorageException;

  /**
   * Removes the shard from storage
   *
   * @param shardId the hash of the shard to remove
   */
  long removeShard(String shardId) throws ShardStorageException;

  /**
   * Lists all available shards
   *
   * @return Set of shard md5 hashes available
   */
  Set<String> listShardIds();

  /**
   * Return the current storage size in bytes
   */
  long getStorageSize();
}
