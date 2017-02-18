package com.domhauton.membrane.config.items;

import java.io.File;

/**
 * Created by Dominic Hauton on 18/02/17.
 */
public class LocalStorageConfig extends StorageConfig {
  public LocalStorageConfig(String storageFolder, int gcInterval, int softStorageLimit, int hardStorageLimit) {
    super(storageFolder, gcInterval, softStorageLimit, hardStorageLimit);
  }

  public LocalStorageConfig() {
    super(System.getProperty("user.home") + File.separator + ".membrane" + File.separator + "local");
  }
}
