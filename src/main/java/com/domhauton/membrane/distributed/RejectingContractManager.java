package com.domhauton.membrane.distributed;

import com.domhauton.membrane.distributed.evidence.EvidenceRequest;
import com.domhauton.membrane.distributed.evidence.EvidenceResponse;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Set;

/**
 * Created by dominic on 30/03/17.
 * <p>
 * A dummy ContractManager Implementation that will return no contracts
 */
public class RejectingContractManager implements ContractManager {

  @Override
  public Set<String> getContractedPeers() {
    return Collections.emptySet();
  }

  @Override
  public int getContractCountTarget() {
    return 0;
  }

  @Override
  public Set<String> getPartiallyDistributedShards() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getFullyDistributedShards() {
    return Collections.emptySet();
  }

  @Override
  public void addContractedPeer(String peerID) throws ContractManagerException {
    throw new ContractManagerException("Cannot add contract to dummy contract manager");
  }

  @Override
  public void receiveBlock(String peerId, String blockId, byte[] data) {
    // Do Nothing
  }

  @Override
  public Set<EvidenceRequest> processPeerContractUpdate(String peerId, DateTime dateTime, int permittedInequality, Set<String> blockIds) {
    return Collections.emptySet();
  }

  @Override
  public void processEvidenceResponses(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses) {
    // Do Nothing
  }

  @Override
  public Set<EvidenceResponse> processEvidenceRequests(String peerId, DateTime dateTime, Set<EvidenceRequest> evidenceRequests) {
    return Collections.emptySet();
  }

  @Override
  public void close() {
    // Do Nothing
  }

  @Override
  public void run() {
    // Do Nothing
  }
}
