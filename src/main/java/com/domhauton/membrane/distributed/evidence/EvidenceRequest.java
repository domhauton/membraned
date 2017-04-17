package com.domhauton.membrane.distributed.evidence;

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

  public EvidenceType getBlock() {
    return block;
  }

  public byte[] getSalt() {
    return salt;
  }
}
