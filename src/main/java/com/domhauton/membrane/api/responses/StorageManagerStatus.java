package com.domhauton.membrane.api.responses;

import java.nio.file.Path;
import java.util.Set;

/**
 * Created by dominic on 04/02/17.
 */
public class StorageManagerStatus implements MembraneResponse {
  private final Set<Path> currentFiles;
  private final Set<Path> referencedFiles;
  private final long localShardStorageSize;
  private final long targetLocalShardStorageSize;
  private final long maxLocalShardStorageSize;
  private final long peerBlockStorageSize;
  private final long targetPeerBlockStorageSize;
  private final long maxPeerBlockStorageSize;

  public StorageManagerStatus(Set<Path> currentFiles, Set<Path> referencedFiles, long localShardStorageSize, long targetLocalShardStorageSize, long maxLocalShardStorageSize, long peerBlockStorageSize, long targetPeerBlockStorageSize, long maxPeerBlockStorageSize) {
    this.currentFiles = currentFiles;
    this.referencedFiles = referencedFiles;
    this.localShardStorageSize = localShardStorageSize;
    this.targetLocalShardStorageSize = targetLocalShardStorageSize;
    this.maxLocalShardStorageSize = maxLocalShardStorageSize;
    this.peerBlockStorageSize = peerBlockStorageSize;
    this.targetPeerBlockStorageSize = targetPeerBlockStorageSize;
    this.maxPeerBlockStorageSize = maxPeerBlockStorageSize;
  }

  public Set<Path> getCurrentFiles() {
    return currentFiles;
  }

  public Set<Path> getReferencedFiles() {
    return referencedFiles;
  }

  public long getLocalShardStorageSize() {
    return localShardStorageSize;
  }

  public long getTargetLocalShardStorageSize() {
    return targetLocalShardStorageSize;
  }

  public long getMaxLocalShardStorageSize() {
    return maxLocalShardStorageSize;
  }

  public long getPeerBlockStorageSize() {
    return peerBlockStorageSize;
  }

  public long getTargetPeerBlockStorageSize() {
    return targetPeerBlockStorageSize;
  }

  public long getMaxPeerBlockStorageSize() {
    return maxPeerBlockStorageSize;
  }
}
