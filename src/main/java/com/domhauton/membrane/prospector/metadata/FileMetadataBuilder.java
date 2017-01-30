package com.domhauton.membrane.prospector.metadata;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.joda.time.DateTime;

/**
 * Created by dominic on 30/01/17.
 */
public class FileMetadataBuilder {
    private byte[] data;
    private final String fullPath;
    private final int chunk;
    private DateTime modifiedTime;

    public FileMetadataBuilder(String fullPath, int chunk, byte[] data) {
        this.fullPath = fullPath;
        this.chunk = chunk;
        this.data = data;

    }

    public FileMetadataBuilder setModifiedTime(DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
        return this;
    }

    public FileMetadata build() {
        DateTime accessTime = DateTime.now();
        HashCode strongHashCode = Hashing.md5().hashBytes(data);
        HashCode weakHashCode = Hashing.crc32().hashBytes(data);
        modifiedTime = modifiedTime == null ? accessTime : modifiedTime;
        return new FileMetadata(fullPath, chunk, modifiedTime, accessTime, strongHashCode, weakHashCode);
    }
}
