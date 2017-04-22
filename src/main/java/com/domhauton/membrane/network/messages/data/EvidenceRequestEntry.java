package com.domhauton.membrane.network.messages.data;

import com.domhauton.membrane.distributed.evidence.EvidenceType;
import org.bouncycastle.util.encoders.Base64;

/**
 * Created by dominic on 20/04/17.
 */
public class EvidenceRequestEntry {
  private String blockId;
  private EvidenceType evidenceType;
  private byte[] salt;

  private EvidenceRequestEntry() {
  } // Jackson ONLY

  public EvidenceRequestEntry(String blockId, EvidenceType evidenceType, byte[] salt) {
    this.blockId = blockId;
    this.evidenceType = evidenceType;
    this.salt = salt;
  }

  public String getBlockId() {
    return blockId;
  }

  public EvidenceType getEvidenceType() {
    return evidenceType;
  }

  public byte[] getSalt() {
    return salt;
  }

  @Override
  public String toString() {
    return "EvidenceRequestEntry{" +
        "blockId='" + blockId + '\'' +
        ", block=" + evidenceType +
        ", salt=" + Base64.toBase64String(salt) +
        '}';
  }
}
