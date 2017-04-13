package com.domhauton.membrane.distributed.appraisal;

import com.domhauton.membrane.distributed.DistributorException;

/**
 * Created by dominic on 13/04/17.
 */
public class AppraisalException extends DistributorException {
  AppraisalException(String s) {
    super(s);
  }

  AppraisalException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
