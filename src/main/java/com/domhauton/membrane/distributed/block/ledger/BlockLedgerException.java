package com.domhauton.membrane.distributed.block.ledger;

import com.domhauton.membrane.distributed.ContractManagerException;

/**
 * Created by dominic on 13/04/17.
 */
public class BlockLedgerException extends ContractManagerException {
  BlockLedgerException(String s) {
    super(s);
  }

  BlockLedgerException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
