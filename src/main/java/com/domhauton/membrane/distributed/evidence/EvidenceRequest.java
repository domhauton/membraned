package com.domhauton.membrane.distributed.evidence;

import java.util.Arrays;

/**
 * Created by dominic on 17/04/17.
 */
public class EvidenceRequest {
  private final String blockId;
  private final EvidenceType block;
  private final byte[] salt;

  public EvidenceRequest(String blockId, EvidenceType block) {
    this(blockId, block, new byte[0]);
  }

  public EvidenceRequest(String blockId, EvidenceType block, byte[] salt) {
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EvidenceRequest that = (EvidenceRequest) o;

    return (getBlockId() != null ? getBlockId().equals(that.getBlockId()) : that.getBlockId() == null) &&
        block == that.block &&
        Arrays.equals(getSalt(), that.getSalt());
  }

  @Override
  public int hashCode() {
    int result = getBlockId() != null ? getBlockId().hashCode() : 0;
    result = 31 * result + (block != null ? block.hashCode() : 0);
    result = 31 * result + Arrays.hashCode(getSalt());
    return result;
  }
}
