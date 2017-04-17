package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.distributed.DistributorException;
import com.domhauton.membrane.distributed.evidence.EvidenceRequest;
import com.domhauton.membrane.distributed.evidence.EvidenceResponse;
import com.google.common.collect.EvictingQueue;
import org.joda.time.DateTime;

import java.util.Collections;
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
    this.size = size;
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

  @Override
  public Set<EvidenceRequest> processPeerContractUpdate(String peerId, DateTime dateTime, int permittedInequality, Set<String> blockIds) {
    return Collections.emptySet();
  }

  @Override
  public void processEvidenceResponse(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
    // Do Nothing
  }

  @Override
  public Set<EvidenceResponse> processEvidenceRequests(String peerId, DateTime dateTime, Set<EvidenceRequest> evidenceRequests) {
    return Collections.emptySet();
  }
}
