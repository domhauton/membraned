package com.domhauton.membrane.distributed.block.ledger.file;

import java.util.List;

/**
 * Created by dominic on 13/04/17.
 */
public class BlockInfoCollection {
  private List<BlockInfoSerializable> blockInfos;

  private BlockInfoCollection() {
  }

  public BlockInfoCollection(List<BlockInfoSerializable> blockInfos) {
    this.blockInfos = blockInfos;
  }

  public List<BlockInfoSerializable> getBlockInfos() {
    return blockInfos;
  }
}
