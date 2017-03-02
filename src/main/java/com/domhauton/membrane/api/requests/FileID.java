package com.domhauton.membrane.api.requests;

/**
 * Created by Dominic Hauton on 25/02/17.
 */
public class FileID {
  private String filepath;
  private String targetFilePath;
  private long dateTimeMillis;

  private FileID() { // Jackson ONLY!
    dateTimeMillis = -1;
  }

  public FileID(String filepath) {
    this(filepath, filepath, -1);
  }

  public FileID(String filepath, String targetFile, long dateTimeMillis) {
    this.filepath = filepath;
    this.dateTimeMillis = dateTimeMillis;
    this.targetFilePath = targetFile;
  }

  public String getFilepath() {
    return filepath;
  }

  public long getDateTimeMillis() {
    return dateTimeMillis;
  }

  public String getTargetFilePath() {
    return targetFilePath;
  }
}
