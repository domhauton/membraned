package com.domhauton.membrane.restful.responses.config;

import com.domhauton.membrane.config.items.StorageConfig;

/**
 * Created by dominic on 04/02/17.
 */
public class StorageConfigREST {
  private int softStorageCap;
  private int hardStorageCap;
  private String directory;
  private int trimFrequency;

  public StorageConfigREST(StorageConfig config) {
    softStorageCap = config.getSoftStorageLimit();
    hardStorageCap = config.getHardStorageLimit();
    directory = config.getStorageFolder();
    trimFrequency = config.getGcInterval();
  }

  public int getSoftStorageCap() {
    return softStorageCap;
  }

  public int getHardStorageCap() {
    return hardStorageCap;
  }

  public String getDirectory() {
    return directory;
  }

  public int getTrimFrequency() {
    return trimFrequency;
  }
}
