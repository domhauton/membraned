package com.domhauton.membrane.distributed;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
public class DistributorException extends Exception {
  public DistributorException(String s) {
    super(s);
  }

  public DistributorException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
