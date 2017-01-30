package com.domhauton.membrane.storage.shard;

import com.google.common.hash.HashCode;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Created by dominic on 30/01/17.
 */
public class ShardStorageImpl {
    Set<String> loadedHash;
    Path basePath;

    public ShardStorageImpl(Path basePath) {
        this.basePath = basePath;
    }

    public void storeShard(String hash, byte[] data) {
        //TODO Impl
    }

    public void retrieveShard(String hash) throws ShardStorageException {
        //TODO Impl
    }

    public void removeShard() {
        //TODO Impl
    }

    public Set<String> listShards() {
        //TODO Impl
        return null;
    }

    private Path getPath(String basePath, String hash) {
        return Paths.get(basePath + File.separator + hash);
    }
}
