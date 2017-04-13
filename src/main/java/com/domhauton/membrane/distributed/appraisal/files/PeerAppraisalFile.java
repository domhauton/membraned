package com.domhauton.membrane.distributed.appraisal.files;

import java.util.List;

/**
 * Created by dominic on 12/04/17.
 */
public class PeerAppraisalFile {
  private UptimeSerializable uptime;
  private List<PeerAppraisalSerializable> peerAppraisals;

  private PeerAppraisalFile() {
  } // JACKSON ONLY

  public PeerAppraisalFile(List<PeerAppraisalSerializable> peerAppraisals, UptimeSerializable uptime) {
    this.peerAppraisals = peerAppraisals;
    this.uptime = uptime;
  }

  public List<PeerAppraisalSerializable> getPeerAppraisals() {
    return peerAppraisals;
  }

  public UptimeSerializable getUptime() {
    return uptime;
  }
}
