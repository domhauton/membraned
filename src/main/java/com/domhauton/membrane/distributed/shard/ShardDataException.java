package com.domhauton.membrane.distributed.shard;

import com.domhauton.membrane.distributed.DistributorException;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
public class ShardDataException extends DistributorException {
  public ShardDataException(String s) {
    super(s);
  }

  public ShardDataException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
