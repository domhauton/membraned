package com.domhauton.membrane.network.messages;

import com.domhauton.membrane.network.NetworkException;

/**
 * Created by Dominic Hauton on 24/02/17.
 */
public class PeerMessageException extends NetworkException {
  PeerMessageException(String s) {
    super(s);
  }
}
