package com.domhauton.membrane.prospector.metadata;

import com.google.common.base.Objects;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import org.joda.time.DateTime;

/**
 * Created by dominic on 26/01/17.
 */
public class FileMetadata {
    private final String fullPath;
    private final int chunk;
    private final DateTime modifiedTime;
    private final DateTime accessTime;
    private final HashCode strongHashCode;
    private final HashCode weakHashCode;

    FileMetadata(
            String fullPath,
            int chunk,
            DateTime modifiedTime,
            DateTime accessTime,
            HashCode strongHashCode,
            HashCode weakHashCode) {
        this.fullPath = fullPath;
        this.chunk = chunk;
        this.modifiedTime = modifiedTime;
        this.accessTime = accessTime;
        this.strongHashCode = strongHashCode;
        this.weakHashCode = weakHashCode;
    }

    public String getFullPath() {
        return fullPath;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public HashCode getStrongHashCode() {
        return strongHashCode;
    }

    private HashCode getWeakHashCode() {
        return weakHashCode;
    }

    public synchronized boolean hashCodeEqual(FileMetadata that) {
        if(that == null) {
            return false;
        }
        boolean weakHashCodeMatch = that.getWeakHashCode().equals(this.getWeakHashCode());
        boolean strongHashCodeMatch = that.getStrongHashCode().equals(this.getStrongHashCode());
        return weakHashCodeMatch && strongHashCodeMatch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMetadata that = (FileMetadata) o;
        return Objects.equal(fullPath, that.fullPath) &&
                Objects.equal(accessTime, that.accessTime) &&
                Objects.equal(strongHashCode, that.strongHashCode) &&
                Objects.equal(weakHashCode, that.weakHashCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullPath, accessTime, strongHashCode, weakHashCode);
    }
}
