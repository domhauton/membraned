package com.domhauton.membrane.restful.responses.config;

import com.domhauton.membrane.config.items.WatcherConfig;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dominic on 04/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class WatcherConfigREST {
  private int fileRescan;
  private int folderRescan;
  private int chunkSize;
  private List<WatchFoldersInfo> watchFolders;

  private WatcherConfigREST() {} // Jackson ONLY

  public WatcherConfigREST(WatcherConfig watcherConfig) {
    this.fileRescan = watcherConfig.getFileRescanInterval();
    this.folderRescan = watcherConfig.getFolderRescanInterval();
    this.watchFolders = watcherConfig.getFolders().stream()
            .map(x -> new WatchFoldersInfo(x.getDirectory(), x.getRecursive()))
            .collect(Collectors.toList());
    this.chunkSize = watcherConfig.getChunkSizeMB();
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
