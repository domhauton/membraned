package com.domhauton.membrane.distributed;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
public class ContractManagerException extends Exception {
  public ContractManagerException(String s) {
    super(s);
  }

  public ContractManagerException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
