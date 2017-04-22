package com.domhauton.membrane.config.items.data;

import com.google.common.base.Objects;

/**
 * Created by dominic on 25/01/17.
 */
public class WatchFolder {
  private String directory;
  private Boolean recursive;

  private WatchFolder() {
  } // Used for jackson loading

  public WatchFolder(String directory, Boolean recursive) {
    this.directory = directory;
    this.recursive = recursive;
  }

  public String getDirectory() {
    return directory;
  }

  public Boolean getRecursive() {
    return recursive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WatchFolder that = (WatchFolder) o;
    return Objects.equal(directory, that.directory) &&
            Objects.equal(recursive, that.recursive);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(directory, recursive);
  }
}
