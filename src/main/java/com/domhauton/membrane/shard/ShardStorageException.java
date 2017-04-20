package com.domhauton.membrane.shard;

/**
 * Created by dominic on 30/01/17.
 */
public class ShardStorageException extends Exception {
  public ShardStorageException(String s) {
    super(s);
  }

  ShardStorageException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
