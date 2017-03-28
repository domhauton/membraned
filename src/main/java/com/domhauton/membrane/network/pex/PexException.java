package com.domhauton.membrane.network.pex;

import com.domhauton.membrane.distributed.DistributorException;

/**
 * Created by dominic on 28/03/17.
 */
public class PexException extends DistributorException {
  public PexException(String s) {
    super(s);
  }

  public PexException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
