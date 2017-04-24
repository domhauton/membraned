package com.domhauton.membrane.api.requests;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by Dominic Hauton on 25/02/17.
 */
public class FileID {
  private String filepath;
  private String targetFilePath;
  private String dateTime;

  private FileID() { // Jackson ONLY!
    dateTime = "";
  }

  public FileID(String filepath) {
    this(filepath, filepath, DateTime.now());
  }

  public FileID(String filepath, String targetFile) {
    this(filepath, targetFile, DateTime.now());
    dateTime = "";
  }

  public FileID(String filepath, String targetFile, DateTime dateTime) {
    this.filepath = filepath;
    this.dateTime = dateTime.toString(ISODateTimeFormat.dateHourMinuteSecondMillis());
    this.targetFilePath = targetFile;
  }

  public String getFilepath() {
    return filepath;
  }

  public String getDateTime() {
    return dateTime;
  }

  public String getTargetFilePath() {
    return targetFilePath;
  }
}
