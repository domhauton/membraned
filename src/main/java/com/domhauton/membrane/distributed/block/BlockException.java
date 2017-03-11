package com.domhauton.membrane.distributed.block;

import com.domhauton.membrane.distributed.DistributorException;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
public class BlockException extends DistributorException {
  public BlockException(String s) {
    super(s);
  }

  public BlockException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
