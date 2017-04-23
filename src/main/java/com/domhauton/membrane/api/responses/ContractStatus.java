package com.domhauton.membrane.api.responses;

import java.util.Set;

/**
 * Created by dominic on 04/02/17.
 */
public class ContractStatus implements MembraneResponse {
  private final boolean contractManagerActive;
  private final int contractTarget;
  private final Set<String> contractedPeers;
  private final Set<String> undeployedShards;
  private final Set<String> partiallyDistributedShards;
  private final Set<String> fullyDistributedShards;

  public ContractStatus(boolean contractManagerActive, int contractTarget, Set<String> contractedPeers, Set<String> undeployedShards, Set<String> partiallyDistributedShards, Set<String> fullyDistributedShards) {
    this.contractManagerActive = contractManagerActive;
    this.contractTarget = contractTarget;
    this.contractedPeers = contractedPeers;
    this.undeployedShards = undeployedShards;
    this.partiallyDistributedShards = partiallyDistributedShards;
    this.fullyDistributedShards = fullyDistributedShards;
  }

  public boolean isContractManagerActive() {
    return contractManagerActive;
  }

  public int getContractTarget() {
    return contractTarget;
  }

  public Set<String> getContractedPeers() {
    return contractedPeers;
  }

  public Set<String> getUndeployedShards() {
    return undeployedShards;
  }

  public Set<String> getPartiallyDistributedShards() {
    return partiallyDistributedShards;
  }

  public Set<String> getFullyDistributedShards() {
    return fullyDistributedShards;
  }
}
