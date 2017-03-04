package com.domhauton.membrane.distributed;

import com.domhauton.membrane.distributed.appraisal.AppraisalLedger;
import com.domhauton.membrane.distributed.contracts.ContractLedger;
import com.domhauton.membrane.distributed.maintainance.UploadRateLimiter;
import com.domhauton.membrane.distributed.manifest.DistributedStore;
import org.joda.time.Duration;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class Distributor {
  private final static Duration RATE_LIMIT = Duration.standardSeconds(5);

  private final DistributedStore distributedStore;
  private final ContractLedger contractLedger;
  private final AppraisalLedger appraisalLedger;
  private final UploadRateLimiter uploadRateLimiter;

  public Distributor() {
    distributedStore = new DistributedStore();
    contractLedger = new ContractLedger();
    appraisalLedger = new AppraisalLedger();
    uploadRateLimiter = new UploadRateLimiter(this::beginUpload, RATE_LIMIT);
  }

  public void storeShard() {

  }

  public void retrieveShards() {

  }

  public void beginUpload() {

  }
}
