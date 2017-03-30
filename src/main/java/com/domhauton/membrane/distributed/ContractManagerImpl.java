package com.domhauton.membrane.distributed;

import java.util.Collections;
import java.util.Set;

/**
 * Created by dominic on 30/03/17.
 * <p>
 * A dummy ContractManager Implementation that will return no contracts
 */
public class ContractManagerImpl implements ContractManager {

  @Override
  public Set<String> getContractedPeers() {
    return Collections.emptySet();
  }

  @Override
  public int getContractCountTarget() {
    return 0;
  }

  @Override
  public void addContractedPeer(String peerID) throws DistributorException {
    throw new DistributorException("Cannot add contract to dummy contract manager");
  }
}
