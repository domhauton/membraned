package com.domhauton.membrane.distributed.block.ledger;

import com.domhauton.membrane.distributed.DistributorException;

/**
 * Created by dominic on 13/04/17.
 */
public class BlockLedgerException extends DistributorException {
  BlockLedgerException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
