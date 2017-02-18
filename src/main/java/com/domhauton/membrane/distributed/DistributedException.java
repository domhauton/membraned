package com.domhauton.membrane.distributed;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
class DistributedException extends Exception {
  DistributedException(String s) {
    super(s);
  }

  DistributedException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
