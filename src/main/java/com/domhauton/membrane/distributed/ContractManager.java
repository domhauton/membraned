package com.domhauton.membrane.distributed;

import java.util.Set;

/**
 * Created by dominic on 30/03/17.
 */
public interface ContractManager {
  Set<String> getContractedPeers();

  int getContractCountTarget();

  void addContractedPeer(String peerId) throws DistributorException;
}
