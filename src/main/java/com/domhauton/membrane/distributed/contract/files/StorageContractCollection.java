package com.domhauton.membrane.distributed.contract.files;

import java.util.List;

/**
 * Created by dominic on 13/04/17.
 */
public class StorageContractCollection {
  private List<StorageContractSerializable> contracts;

  private StorageContractCollection() {
  } // Jackson ONLY

  public StorageContractCollection(List<StorageContractSerializable> contracts) {
    this.contracts = contracts;
  }

  public List<StorageContractSerializable> getContracts() {
    return contracts;
  }
}
