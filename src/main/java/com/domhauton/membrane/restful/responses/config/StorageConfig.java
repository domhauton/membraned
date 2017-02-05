package com.domhauton.membrane.restful.responses.config;

/**
 * Created by dominic on 04/02/17.
 */
public class StorageConfig {
    private int softStorageCap;
    private int hardStorageCap;
    private String directory;
    private int trimFrequency;

    public StorageConfig(int softStorageCap, int hardStorageCap, String directory, int trimFrequency) {
        this.softStorageCap = softStorageCap;
        this.hardStorageCap = hardStorageCap;
        this.directory = directory;
        this.trimFrequency = trimFrequency;
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
