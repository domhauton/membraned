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
  private DateTime modifiedTime;
  private final List<MD5HashLengthPair> md5HashLengthPairs;

  public FileMetadataBuilder(DateTime modifiedTime) {
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
    modifiedTime = modifiedTime == null ? DateTime.now() : modifiedTime;
    return new FileMetadata(modifiedTime, md5HashLengthPairs);
  }
}
