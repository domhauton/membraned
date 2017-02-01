package com.domhauton.membrane.prospector.metadata;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by dominic on 30/01/17.
 */
public class FileMetadataBuilder {
    private final String fullPath;
    private DateTime modifiedTime;
    private List<String> md5Hashes;

    public FileMetadataBuilder(String fullPath, DateTime modifiedTime) {
        this.fullPath = fullPath;
        this.modifiedTime = modifiedTime;
        md5Hashes = new LinkedList<>();
    }

    public String addHashData(byte[] data) {
        String md5Hash = Hashing.md5().hashBytes(data).toString();
        md5Hashes.add(md5Hash);
        return md5Hash;
    }

    public FileMetadataBuilder addShardList(List<String> md5Hashes) {
        this.md5Hashes.addAll(md5Hashes);
        return this;
    }

    public FileMetadata build() {
        DateTime accessTime = DateTime.now();
        modifiedTime = modifiedTime == null ? accessTime : modifiedTime;
        return new FileMetadata(fullPath, modifiedTime, accessTime, md5Hashes);
    }
}
