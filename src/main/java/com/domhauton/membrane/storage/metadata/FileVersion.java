package com.domhauton.membrane.storage.metadata;

import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by dominic on 30/01/17.
 */
public class FileVersion {
    private final List<String> shardHash;
    private final DateTime modificationDateTime;
    private final Path storedPath;

    public FileVersion(List<String> shardHash, DateTime modificationDateTime, Path storedPath) {
        this.shardHash = shardHash;
        this.modificationDateTime = modificationDateTime;
        this.storedPath = storedPath;
    }

    public List<String> getShardHash() {
        return shardHash;
    }

    public DateTime getModificationDateTime() {
        return modificationDateTime;
    }

    public Path getStoredPath() {
        return storedPath;
    }
}
