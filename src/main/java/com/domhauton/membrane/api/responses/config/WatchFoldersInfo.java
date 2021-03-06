package com.domhauton.membrane.api.responses.config;

/**
 * Created by dominic on 04/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class WatchFoldersInfo {
  private String directory;
  private boolean recursive;

  private WatchFoldersInfo() {} // Jackson ONLY

  public WatchFoldersInfo(String directory, boolean recursive) {
    this.directory = directory;
    this.recursive = recursive;
  }

  public String getDirectory() {
    return directory;
  }

  public boolean isRecursive() {
    return recursive;
  }
}
