package com.domhauton.membrane.distributed.manifest;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
class DistributedShard {

  private final String md5Hash;
  private Priority priority;
  private Set<String> storedByPeers;

  DistributedShard(String md5Hash, Priority priority) {
    this.md5Hash = md5Hash;
    this.priority = priority;
    storedByPeers = new HashSet<>();
  }

  void upgradePriority(Priority priority) {
    if (priority.getValue() > this.priority.getValue()) {
      this.priority = priority;
    }
  }

  int requiredPeers() {
    return priority.getRequiredCopies() - storedByPeers.size();
  }

  void addPeer(String peer) {
    storedByPeers.add(peer);
  }

  void removePeer(String peer) {
    storedByPeers.remove(peer);
  }

  boolean isStoredBy(String peer) {
    return storedByPeers.contains(peer);
  }

  public String getMd5Hash() {
    return md5Hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DistributedShard that = (DistributedShard) o;

    return (md5Hash != null ? md5Hash.equals(that.md5Hash) : that.md5Hash == null) &&
            priority == that.priority && (storedByPeers != null ? storedByPeers.equals(that.storedByPeers) : that.storedByPeers == null);
  }

  @Override
  public int hashCode() {
    int result = md5Hash != null ? md5Hash.hashCode() : 0;
    result = 31 * result + (priority != null ? priority.hashCode() : 0);
    result = 31 * result + (storedByPeers != null ? storedByPeers.hashCode() : 0);
    return result;
  }
}
