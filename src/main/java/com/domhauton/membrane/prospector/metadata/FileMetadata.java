package com.domhauton.membrane.prospector.metadata;

import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import com.google.common.base.Objects;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by dominic on 26/01/17.
 */
public class FileMetadata {
    private final String fullPath;
    private final DateTime modifiedTime;
    private final DateTime accessTime;
    private final List<MD5HashLengthPair> md5HashLengthPairs;

    public FileMetadata(String fullPath, DateTime modifiedTime, DateTime accessTime, List<MD5HashLengthPair> md5HashLengthPairs) {
        this.fullPath = fullPath;
        this.modifiedTime = modifiedTime;
        this.accessTime = accessTime;
        this.md5HashLengthPairs = md5HashLengthPairs;
    }

    public String getFullPath() {
        return fullPath;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public DateTime getAccessTime() {
        return accessTime;
    }

    public List<MD5HashLengthPair> getMd5HashLengthPairs() {
        return md5HashLengthPairs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMetadata that = (FileMetadata) o;
        return Objects.equal(fullPath, that.fullPath) &&
                Objects.equal(modifiedTime, that.modifiedTime) &&
                Objects.equal(accessTime, that.accessTime) &&
                Objects.equal(md5HashLengthPairs, that.md5HashLengthPairs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullPath, modifiedTime, accessTime, md5HashLengthPairs);
    }
}
