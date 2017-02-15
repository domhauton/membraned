package com.domhauton.membrane.restful.responses.config;

import java.util.List;

/**
 * Created by dominic on 04/02/17.
 */
public class WatcherConfig {
  private int fileRescan;
  private int folderRescan;
  private int chunkSize;
  private List<WatchFoldersInfo> watchFolders;

  public WatcherConfig(int fileRescan, int folderRescan, int chunkSize, List<WatchFoldersInfo> watchFolders) {
    this.fileRescan = fileRescan;
    this.folderRescan = folderRescan;
    this.chunkSize = chunkSize;
    this.watchFolders = watchFolders;
  }

  public int getFileRescan() {
    return fileRescan;
  }

  public int getFolderRescan() {
    return folderRescan;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public List<WatchFoldersInfo> getWatchFolders() {
    return watchFolders;
  }
}
