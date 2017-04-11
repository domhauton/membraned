package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.DistributorException;
import com.google.common.collect.EvictingQueue;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by dominic on 11/04/17.
 * <p>
 * For use in tests only. Emulates the behaviour of functional contract manager.
 */
class EvictingContractManager implements ContractManager {

  private EvictingQueue<String> peerQueue;
  private int size;

  public EvictingContractManager(int size) {
    peerQueue = EvictingQueue.create(size);
  }

  @Override
  public Set<String> getContractedPeers() {
    return new HashSet<>(peerQueue);
  }

  @Override
  public int getContractCountTarget() {
    return size;
  }

  @Override
  public void addContractedPeer(String peerId) throws DistributorException {
    synchronized (this) {
      if (!peerQueue.contains(peerId)) {
        peerQueue.add(peerId);
      }
    }
  }
}
