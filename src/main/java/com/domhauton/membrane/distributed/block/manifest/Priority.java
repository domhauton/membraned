package com.domhauton.membrane.distributed.block.manifest;

/**
 * Created by Dominic Hauton on 03/03/17.
 */
public enum Priority {
  Critical(200, 6),
  Normal(150, 3);

  private final int value;
  private final int requiredCopies;

  Priority(int value, int requiredCopies) {
    this.value = value;
    this.requiredCopies = requiredCopies;
  }

  public int getValue() {
    return value;
  }

  public int getRequiredCopies() {
    return requiredCopies;
  }
}
