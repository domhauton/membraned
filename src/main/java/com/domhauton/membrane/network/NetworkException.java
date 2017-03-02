package com.domhauton.membrane.network;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class NetworkException extends Exception {
  protected NetworkException(String s) {
    super(s);
  }

  protected NetworkException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
