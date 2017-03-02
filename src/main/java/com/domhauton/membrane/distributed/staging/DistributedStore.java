package com.domhauton.membrane.distributed.staging;

import java.util.LinkedList;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
public class DistributedStore {
  private LinkedList<DistributedShard> distributedShards;

  public DistributedStore() {
    this.distributedShards = new LinkedList<>();
  }
}
