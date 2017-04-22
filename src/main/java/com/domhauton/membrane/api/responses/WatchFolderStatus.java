package com.domhauton.membrane.api.responses;

import com.domhauton.membrane.api.responses.config.WatchFoldersInfo;
import com.domhauton.membrane.config.items.data.WatchFolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dominic on 04/02/17.
 */
@SuppressWarnings("CanBeFinal")
public class WatchFolderStatus implements MembraneResponse {
  private List<WatchFoldersInfo> watchFolders;

  private WatchFolderStatus() {
  } // Jackson ONLY

  public WatchFolderStatus(List<WatchFolder> watchFolders) {
    this.watchFolders = watchFolders.stream()
            .map(x -> new WatchFoldersInfo(x.getDirectory(), x.getRecursive()))
            .collect(Collectors.toList());
  }

  public List<WatchFoldersInfo> getWatchFolders() {
    return watchFolders;
  }
}
