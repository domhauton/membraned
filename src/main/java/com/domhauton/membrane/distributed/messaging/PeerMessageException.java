package com.domhauton.membrane.distributed.messaging;

import com.domhauton.membrane.distributed.DistributedException;

/**
 * Created by Dominic Hauton on 24/02/17.
 */
public class PeerMessageException extends DistributedException {
  PeerMessageException(String s) {
    super(s);
  }

  PeerMessageException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
