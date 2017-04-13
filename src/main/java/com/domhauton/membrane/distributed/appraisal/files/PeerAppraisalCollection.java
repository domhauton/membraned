package com.domhauton.membrane.distributed.appraisal.files;

import java.util.List;

/**
 * Created by dominic on 12/04/17.
 */
public class PeerAppraisalCollection {
  private List<PeerAppraisalSerializable> peerAppraisals;

  private PeerAppraisalCollection() {
  } // JACKSON ONLY

  public PeerAppraisalCollection(List<PeerAppraisalSerializable> peerAppraisals) {
    this.peerAppraisals = peerAppraisals;
  }

  public List<PeerAppraisalSerializable> getPeerAppraisals() {
    return peerAppraisals;
  }
}
