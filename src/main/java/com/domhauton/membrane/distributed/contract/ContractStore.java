package com.domhauton.membrane.distributed.contract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dominic Hauton on 11/03/17.
 */
public class ContractStore {
  private final List<StorageContract> contractList;

  public ContractStore() {
    contractList = new ArrayList<>();
  }
}
