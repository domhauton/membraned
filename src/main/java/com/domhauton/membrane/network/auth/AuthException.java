package com.domhauton.membrane.network.auth;

/**
 * Created by dominic on 11/02/17.
 */
public class AuthException extends Exception {
  AuthException(String s) {
    super(s);
  }

  AuthException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
