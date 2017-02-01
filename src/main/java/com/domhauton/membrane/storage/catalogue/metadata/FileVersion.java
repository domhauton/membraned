package com.domhauton.membrane.storage.catalogue.metadata;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by dominic on 30/01/17.
 */
public class FileVersion {
    private final List<String> shardHash;
    private final DateTime modificationDateTime;

    public FileVersion(List<String> shardHash, DateTime modificationDateTime) {
        this.shardHash = ImmutableList.copyOf(shardHash);
        this.modificationDateTime = modificationDateTime;
    }

    public List<String> getMD5ShardList() {
        return shardHash;
    }

    public DateTime getModificationDateTime() {
        return modificationDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileVersion that = (FileVersion) o;
        return Objects.equal(shardHash, that.shardHash) &&
                Objects.equal(modificationDateTime, that.modificationDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(shardHash, modificationDateTime);
    }
}
