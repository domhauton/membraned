package com.domhauton.membrane.prospector.metadata;

import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
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
    private List<MD5HashLengthPair> md5HashLengthPairs;

    public FileMetadataBuilder(String fullPath, DateTime modifiedTime) {
        this.fullPath = fullPath;
        this.modifiedTime = modifiedTime;
        md5HashLengthPairs = new LinkedList<>();
    }

    public String addHashData(byte[] data) {
        String md5Hash = Hashing.md5().hashBytes(data).toString();
        MD5HashLengthPair md5HashLengthPair = new MD5HashLengthPair(md5Hash, data.length);
        md5HashLengthPairs.add(md5HashLengthPair);
        return md5Hash;
    }

    public FileMetadataBuilder addShardData(List<MD5HashLengthPair> md5HashLengthPairs) {
        this.md5HashLengthPairs.addAll(md5HashLengthPairs);
        return this;
    }

    public FileMetadata build() {
        DateTime accessTime = DateTime.now();
        modifiedTime = modifiedTime == null ? accessTime : modifiedTime;
        return new FileMetadata(fullPath, modifiedTime, accessTime, md5HashLengthPairs);
    }
}
