package com.domhauton.membrane.network.messages.data;

import com.domhauton.membrane.distributed.evidence.EvidenceType;
import org.bouncycastle.util.encoders.Base64;

/**
 * Created by dominic on 20/04/17.
 */
public class EvidenceResponseEntry {
  private String blockId;
  private EvidenceType evidenceType;
  private byte[] response;

  private EvidenceResponseEntry() {
  } // Jackson ONLY

  public EvidenceResponseEntry(String blockId, EvidenceType evidenceType, byte[] response) {
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
  public String toString() {
    return "EvidenceResponseEntry{" +
        "blockId='" + blockId + '\'' +
        ", evidenceType=" + evidenceType +
        ", response=" + Base64.toBase64String(response) +
        '}';
  }
}
