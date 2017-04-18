package com.domhauton.membrane.distributed.block.gen;

import com.domhauton.membrane.distributed.DistributorException;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
public class BlockException extends DistributorException {
  BlockException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
