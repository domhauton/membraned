package com.domhauton.membrane.prospector;

import com.google.common.base.Objects;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import org.joda.time.DateTime;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by dominic on 26/01/17.
 */
public class FileMetadata {
    private final String fullPath;
    private final DateTime accessTime;
    private final HashCode strongHashCode;
    private final HashCode weakHashCode;

    public FileMetadata(String filePath, byte[] content) {
        this.fullPath = filePath;
        accessTime = DateTime.now();
        strongHashCode = Hashing.md5().hashBytes(content);
        weakHashCode = Hashing.crc32().hashBytes(content);
    }

    public String getFullPath() {
        return fullPath;
    }

    public DateTime getAccessTime() {
        return accessTime;
    }

    synchronized boolean hashCodeEqual(FileMetadata that) {
        if(that == null) {
            return false;
        }
        boolean weakHashCodeMatch = that.getWeakHashCode().equals(this.getWeakHashCode());
        boolean strongHashCodeMatch = that.getStrongHashCode().equals(this.getStrongHashCode());
        return weakHashCodeMatch && strongHashCodeMatch;
    }

    public HashCode getStrongHashCode() {
        return strongHashCode;
    }

    public HashCode getWeakHashCode() {
        return weakHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileMetadata)) return false;
        FileMetadata that = (FileMetadata) o;
        return Objects.equal(getFullPath(), that.getFullPath()) &&
                Objects.equal(byteSource, that.byteSource) &&
                Objects.equal(getAccessTime(), that.getAccessTime()) &&
                Objects.equal(getStrongHashCode(), that.getStrongHashCode()) &&
                Objects.equal(getWeakHashCode(), that.getWeakHashCode());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getFullPath(), byteSource, getAccessTime(), getStrongHashCode(), getWeakHashCode());
    }
}
