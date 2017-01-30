package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.shard.ShardStorageImpl;
import com.google.common.hash.HashCode;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by dominic on 30/01/17.
 */
public class StorageManager {
    private ShardStorageImpl shardStorage;
    private FileCatalogue fileCatalogue;

    public StorageManager() {
    }

    public void storeShard(HashCode hashCode, byte[] data) {
        shardStorage.storeShard(hashCode.toString(), data);
    }

    public void addFile(List<String> shardHash, DateTime modificationDateTime, Path storedPath) {
        //TODO: (?) Check storage for existence of all shards
        fileCatalogue.addFile(shardHash, modificationDateTime, storedPath);
    }

    public void removeFile(List<String> shardHash, DateTime modificationDateTime, Path storedPath) {
        fileCatalogue.addFile(shardHash, modificationDateTime, storedPath);
    }

    public void rebuildFile(Path storedPath) {
        // Search for stored file
        // Put file in FS with given length.
        // Copy over a shard at a time.
        // Set correct permissions
    }

    public void collectGarbage() {
        // Get shardList from storage manager
        // Prune catalogue
        // Find which shards are not in the catalogue
        // Remove
    }
}
