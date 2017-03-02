package com.domhauton.membrane.api.responses;

import java.util.List;

/**
 * Created by dominic on 05/02/17.
 */
public class FileHistoryEntry {
  private final long dateTimeMillis;
  private final List<String> hashes;
  private final long size;
  private final boolean isRemove;

  public FileHistoryEntry(long dateTimeMillis, List<String> hashes, long size, boolean isRemove) {
    this.dateTimeMillis = dateTimeMillis;
    this.hashes = hashes;
    this.size = size;
    this.isRemove = isRemove;
  }

  public long getDateTime() {
    return dateTimeMillis;
  }

  public List<String> getMD5Hashes() {
    return hashes;
  }

  public long getSize() {
    return size;
  }

  public boolean isRemove() {
    return isRemove;
  }
}
