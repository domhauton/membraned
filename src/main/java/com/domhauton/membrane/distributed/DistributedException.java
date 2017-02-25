package com.domhauton.membrane.distributed;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class DistributedException extends Exception {
  protected DistributedException(String s) {
    super(s);
  }

  protected DistributedException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
