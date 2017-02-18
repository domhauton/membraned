package com.domhauton.membrane.restful.responses;

import java.util.Set;

/**
 * Created by dominic on 03/02/17.
 */
public class FileManagerStatus implements MembraneResponse {
  private final Set<String> trackedFolders;
  private final Set<String> trackedFiles;

  public FileManagerStatus(Set<String> trackedFolders, Set<String> trackedFiles) {
    this.trackedFolders = trackedFolders;
    this.trackedFiles = trackedFiles;
  }

  public Set<String> getTrackedFolders() {
    return trackedFolders;
  }

  public Set<String> getTrackedFiles() {
    return trackedFiles;
  }
}
