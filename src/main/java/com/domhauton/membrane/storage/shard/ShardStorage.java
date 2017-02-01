package com.domhauton.membrane.storage.shard;

import java.nio.file.Path;
import java.util.Set;

/**
 * Created by dominic on 31/01/17.
 */
public interface ShardStorage {
    /**
     * Store the data given give.
     * @param md5Hash The md5Hash of the data given
     * @param data The data to store.
     * @throws ShardStorageException
     */
    public void storeShard(String md5Hash, byte[] data) throws ShardStorageException;

    /**
     * Retrieves a shard from storage and checks consistency
     * @param md5Hash md5Hash of requested shard
     * @return data requested.
     * @throws ShardStorageException If cannot access shard, shard does not exist or file corrupt.
     */
    public byte[] retrieveShard(String md5Hash) throws ShardStorageException;

    /**
     * Removes the shard from storage
     * @param md5Hash the hash of the shard to remove
     * @throws ShardStorageException
     */
    public void removeShard(String md5Hash) throws ShardStorageException;

    /**
     * Lists all available shards
     * @return Set of shard md5 hashes available
     */
    public Set<String> listShards();
}
