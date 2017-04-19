package com.domhauton.membrane.distributed.evidence;

import java.util.Arrays;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EvidenceResponse that = (EvidenceResponse) o;

    return (getBlockId() != null ? getBlockId().equals(that.getBlockId()) : that.getBlockId() == null) &&
        getEvidenceType() == that.getEvidenceType() &&
        Arrays.equals(getResponse(), that.getResponse());
  }

  @Override
  public int hashCode() {
    int result = getBlockId() != null ? getBlockId().hashCode() : 0;
    result = 31 * result + (getEvidenceType() != null ? getEvidenceType().hashCode() : 0);
    result = 31 * result + Arrays.hashCode(getResponse());
    return result;
  }
}
