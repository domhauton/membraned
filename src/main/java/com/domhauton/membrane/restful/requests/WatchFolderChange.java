package com.domhauton.membrane.restful.requests;

import com.domhauton.membrane.config.items.WatchFolder;

/**
 * Created by Dominic Hauton on 17/02/17.
 */
public class WatchFolderChange {
  private Type type;
  private WatchFolder watchFolder;

  public WatchFolderChange() {} // For Jackson

  public WatchFolderChange(Type type, WatchFolder watchFolder) {
    this.type = type;
    this.watchFolder = watchFolder;
  }

  public Type getType() {
    return type;
  }

  public WatchFolder getWatchFolder() {
    return watchFolder;
  }

  public enum Type {
    ADD, REMOVE
  }
}