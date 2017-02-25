package com.domhauton.membrane.prospector.metadata;

import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by dominic on 26/01/17.
 */
public class FileMetadata {
  private final DateTime modifiedTime;
  private final List<MD5HashLengthPair> md5HashLengthPairs;

  public FileMetadata(DateTime modifiedTime, List<MD5HashLengthPair> md5HashLengthPairs) {
    this.modifiedTime = modifiedTime;
    this.md5HashLengthPairs = md5HashLengthPairs;
  }

  public DateTime getModifiedTime() {
    return modifiedTime;
  }

  public List<MD5HashLengthPair> getMd5HashLengthPairs() {
    return md5HashLengthPairs;
  }
}
