package com.domhauton.membrane.api.responses;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;

/**
 * Created by dominic on 05/02/17.
 */
public class FileHistoryEntry {
  private final String dateTime;
  private final List<String> hashes;
  private final long size;
  private final boolean isRemove;

  public FileHistoryEntry(DateTime dateTime, List<String> hashes, long size, boolean isRemove) {
    this.dateTime = dateTime.toString(ISODateTimeFormat.dateHourMinuteSecondMillis());
    this.hashes = hashes;
    this.size = size;
    this.isRemove = isRemove;
  }

  public String getDateTime() {
    return dateTime;
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
