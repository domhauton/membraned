package com.domhauton.membrane.distributed.evidence;

/**
 * Created by dominic on 17/04/17.
 */
public class EvidenceResponse {
  private final String blockId;
  private final EvidenceType evidenceType;
  private final byte[] response;

  public EvidenceResponse(String blockId, EvidenceType evidenceType, byte[] response) {
    this.blockId = blockId;
    this.evidenceType = evidenceType;
    this.response = response;
  }

  public String getBlockId() {
    return blockId;
  }

  public EvidenceType getEvidenceType() {
    return evidenceType;
  }

  public byte[] getResponse() {
    return response;
  }
}
