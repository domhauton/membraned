package com.domhauton.membrane.distributed.contract;

import com.domhauton.membrane.distributed.ContractManagerException;

/**
 * Created by Dominic Hauton on 11/03/17.
 */
public class ContractStoreException extends ContractManagerException {
  ContractStoreException(String s) {
    super(s);
  }

  ContractStoreException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
