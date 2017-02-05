package com.domhauton.membrane.storage.catalogue.metadata;

import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 */
public class FileVersion {
    private final List<MD5HashLengthPair> MD5HashLengthPairs;
    private final DateTime modificationDateTime;

    public FileVersion(List<MD5HashLengthPair> MD5HashLengthPairs, DateTime modificationDateTime) {
        this.MD5HashLengthPairs = MD5HashLengthPairs;
        this.modificationDateTime = modificationDateTime;
    }

    public List<MD5HashLengthPair> getMD5HashLengthPairs() {
        return MD5HashLengthPairs;
    }

    public List<String> getMD5HashList() {
        return getMD5HashLengthPairs().stream().map(MD5HashLengthPair::getMd5Hash).collect(Collectors.toList());
    }

    public long getTotalSize() {
        return getMD5HashLengthPairs().stream().mapToLong(MD5HashLengthPair::getLength).sum();
    }

    public DateTime getModificationDateTime() {
        return modificationDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileVersion that = (FileVersion) o;
        return Objects.equal(MD5HashLengthPairs, that.MD5HashLengthPairs) &&
                Objects.equal(modificationDateTime, that.modificationDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(MD5HashLengthPairs, modificationDateTime);
    }
}
