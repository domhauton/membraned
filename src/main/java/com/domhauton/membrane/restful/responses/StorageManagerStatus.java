package com.domhauton.membrane.restful.responses;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 04/02/17.
 */
public class StorageManagerStatus implements MembraneResponse {
  private Set<Path> currentFiles;
  private Set<Path> referencedFiles;
  private Long storageSize;

  public StorageManagerStatus(Set<Path> currentFiles, Set<Path> trackedFiles, Long storageSize) {
    this.currentFiles = currentFiles;
    this.referencedFiles = trackedFiles;
    this.storageSize = storageSize;
  }

  public Set<String> getCurrentFiles() {
    return currentFiles.stream().map(Path::toString).collect(Collectors.toSet());
  }

  public Set<String> getReferencedFiles() {
    return referencedFiles.stream().map(Path::toString).collect(Collectors.toSet());
  }

  public Long getStorageSize() {
    return storageSize;
  }
}
