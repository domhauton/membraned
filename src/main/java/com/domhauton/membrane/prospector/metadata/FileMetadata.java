package com.domhauton.membrane.prospector.metadata;

import com.google.common.base.Objects;
import com.google.common.hash.HashCode;

import org.joda.time.DateTime;

/**
 * Created by dominic on 26/01/17.
 */
public class FileMetadata {
    private final String fullPath;
    private final int chunk;
    private final DateTime modifiedTime;
    private final DateTime accessTime;
    private final HashCode strongHash;
    private final HashCode weakHash;

    FileMetadata(
            String fullPath,
            int chunk,
            DateTime modifiedTime,
            DateTime accessTime,
            HashCode strongHash,
            HashCode weakHash) {
        this.fullPath = fullPath;
        this.chunk = chunk;
        this.modifiedTime = modifiedTime;
        this.accessTime = accessTime;
        this.strongHash = strongHash;
        this.weakHash = weakHash;
    }

    public String getFullPath() {
        return fullPath;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public HashCode getStrongHash() {
        return strongHash;
    }

    private HashCode getWeakHash() {
        return weakHash;
    }

    public synchronized boolean hashCodeEqual(FileMetadata that) {
        if(that == null) {
            return false;
        }
        boolean weakHashCodeMatch = that.getWeakHash().equals(this.getWeakHash());

        return weakHashCodeMatch && that.getStrongHash().equals(this.getStrongHash());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMetadata that = (FileMetadata) o;
        return Objects.equal(fullPath, that.fullPath) &&
                Objects.equal(accessTime, that.accessTime) &&
                Objects.equal(strongHash, that.strongHash) &&
                Objects.equal(weakHash, that.weakHash);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullPath, accessTime, strongHash, weakHash);
    }
}
