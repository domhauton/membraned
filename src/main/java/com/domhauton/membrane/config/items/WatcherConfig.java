package com.domhauton.membrane.config.items;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class WatcherConfig {
  private int chunkSizeMB;
  private List<WatchFolder> folders;
  private int fileRescanInterval;
  private int folderRescanInterval;

  public WatcherConfig(int chunkSizeMB, List<WatchFolder> folders, int fileRescanInterval, int folderRescanInterval) {
    this.chunkSizeMB = chunkSizeMB;
    this.folders = folders;
    this.fileRescanInterval = fileRescanInterval;
    this.folderRescanInterval = folderRescanInterval;
  }

  public WatcherConfig() {
    folders = new ArrayList<>();
    chunkSizeMB = 64;
    fileRescanInterval = 20;
    folderRescanInterval = 120;
  }

  public int getChunkSizeMB() {
    return chunkSizeMB;
  }

  public List<WatchFolder> getFolders() {
    return folders;
  }

  public int getFileRescanInterval() {
    return fileRescanInterval;
  }

  public int getFolderRescanInterval() {
    return folderRescanInterval;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WatcherConfig that = (WatcherConfig) o;

    return chunkSizeMB == that.chunkSizeMB &&
            fileRescanInterval == that.fileRescanInterval &&
            folderRescanInterval == that.folderRescanInterval &&
            (folders != null ? folders.equals(that.folders) : that.folders == null);
  }
}
