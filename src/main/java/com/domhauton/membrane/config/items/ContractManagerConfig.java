package com.domhauton.membrane.config.items;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class ContractManagerConfig {
  private boolean active;
  private int targetContractCount;
  private boolean searchForNewPeers;

  public ContractManagerConfig() {
    active = true;
    targetContractCount = 100;
    searchForNewPeers = true;
  }

  public ContractManagerConfig(boolean active, int targetContractCount, boolean searchForNewPeers) {
    this.active = active;
    this.targetContractCount = targetContractCount;
    this.searchForNewPeers = searchForNewPeers;
  }

  public boolean isActive() {
    return active;
  }

  public int getTargetContractCount() {
    return targetContractCount;
  }

  public boolean isSearchForNewPeers() {
    return searchForNewPeers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContractManagerConfig that = (ContractManagerConfig) o;

    return isActive() == that.isActive();
  }
}
