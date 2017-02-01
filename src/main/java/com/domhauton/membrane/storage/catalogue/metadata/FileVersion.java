package com.domhauton.membrane.storage.catalogue.metadata;

import com.google.common.base.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileVersion that = (FileVersion) o;
        return Objects.equal(shardHash, that.shardHash) &&
                Objects.equal(modificationDateTime, that.modificationDateTime) &&
                Objects.equal(storedPath, that.storedPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(shardHash, modificationDateTime, storedPath);
    }
}
