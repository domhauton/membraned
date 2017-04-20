package com.domhauton.membrane.network.messages.data;

import com.domhauton.membrane.distributed.evidence.EvidenceType;
import org.bouncycastle.util.encoders.Base64;

/**
 * Created by dominic on 20/04/17.
 */
public class EvidenceRequestEntry {
  private String blockId;
  private EvidenceType block;
  private byte[] salt;

  private EvidenceRequestEntry() {
  } // Jackson ONLY

  public EvidenceRequestEntry(String blockId, EvidenceType block, byte[] salt) {
    this.blockId = blockId;
    this.block = block;
    this.salt = salt;
  }

  public String getBlockId() {
    return blockId;
  }

  public EvidenceType getEvidenceType() {
    return block;
  }

  public byte[] getSalt() {
    return salt;
  }

  @Override
  public String toString() {
    return "EvidenceRequestEntry{" +
        "blockId='" + blockId + '\'' +
        ", block=" + block +
        ", salt=" + Base64.toBase64String(salt) +
        '}';
  }
}
