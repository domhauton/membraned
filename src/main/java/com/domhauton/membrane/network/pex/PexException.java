package com.domhauton.membrane.network.pex;

import com.domhauton.membrane.network.NetworkException;

/**
 * Created by dominic on 28/03/17.
 */
public class PexException extends NetworkException {
  public PexException(String s) {
    super(s);
  }

  PexException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
