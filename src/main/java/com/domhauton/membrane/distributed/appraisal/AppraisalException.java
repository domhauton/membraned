package com.domhauton.membrane.distributed.appraisal;

import com.domhauton.membrane.distributed.ContractManagerException;

/**
 * Created by dominic on 13/04/17.
 */
public class AppraisalException extends ContractManagerException {
  AppraisalException(String s) {
    super(s);
  }

  AppraisalException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
