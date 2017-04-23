package com.domhauton.membrane.distributed;

import com.domhauton.membrane.distributed.evidence.EvidenceRequest;
import com.domhauton.membrane.distributed.evidence.EvidenceResponse;
import org.joda.time.DateTime;

import java.io.Closeable;
import java.util.Set;

/**
 * Created by dominic on 30/03/17.
 */
public interface ContractManager extends Runnable, Closeable {
  Set<String> getContractedPeers();

  int getContractCountTarget();

  Set<String> getPartiallyDistributedShards();

  Set<String> getFullyDistributedShards();

  void addContractedPeer(String peerId) throws ContractManagerException;

  void receiveBlock(String peerId, String blockId, byte[] data);

  Set<EvidenceRequest> processPeerContractUpdate(String peerId, DateTime dateTime, int permittedInequality, Set<String> blockIds);

  void processEvidenceResponses(String peerId, DateTime dateTime, Set<EvidenceResponse> evidenceResponses);

  Set<EvidenceResponse> processEvidenceRequests(String peerId, DateTime dateTime, Set<EvidenceRequest> evidenceRequests);

  void close();
}
