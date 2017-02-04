package com.domhauton.membrane.config;

import com.domhauton.membrane.config.items.WatchFolder;
import com.google.common.base.Objects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dominic on 23/01/17.
 */
public class Config {
    private String shardStorageFolder;
    private int fileRescanFrequencySec;
    private int folderRescanFrequencySec;
    private int storageTrimFrequencyMin;
    private int chunkSizeMB;
    private int garbageCollectThresholdMB;
    private int maxStorageSizeMB;
    private List<WatchFolder> folders;
    private int verticlePort;

    public Config() {
        folders = new ArrayList<>();
        shardStorageFolder = System.getProperty("user.home") + File.separator + ".membrane";
        fileRescanFrequencySec = 10;
        folderRescanFrequencySec = 120;
        storageTrimFrequencyMin = 20;
        chunkSizeMB = 64;
        maxStorageSizeMB = 4096;
        garbageCollectThresholdMB = 2048;
        verticlePort = 13200;
    }

    public Config(String shardStorageFolder, int fileRescanFrequencySec, int folderRescanFrequencySec, int storageTrimFrequencyMin, int chunkSizeMB, int garbageCollectThresholdMB, int maxStorageSizeMB, List<WatchFolder> folders, int verticlePort) {
        this.shardStorageFolder = shardStorageFolder;
        this.fileRescanFrequencySec = fileRescanFrequencySec;
        this.folderRescanFrequencySec = folderRescanFrequencySec;
        this.storageTrimFrequencyMin = storageTrimFrequencyMin;
        this.chunkSizeMB = chunkSizeMB;
        this.garbageCollectThresholdMB = garbageCollectThresholdMB;
        this.maxStorageSizeMB = maxStorageSizeMB;
        this.folders = folders;
        this.verticlePort = verticlePort;
    }

    public List<WatchFolder> getFolders() {
        return folders;
    }

    public String getShardStorageFolder() {
        return shardStorageFolder;
    }

    public int getFileRescanFrequencySec() {
        return fileRescanFrequencySec;
    }

    public int getFolderRescanFrequencySec() {
        return folderRescanFrequencySec;
    }

    public int getChunkSizeMB() {
        return chunkSizeMB;
    }

    public int getMaxStorageSizeMB() {
        return maxStorageSizeMB;
    }

    public int getStorageTrimFrequencyMin() {
        return storageTrimFrequencyMin;
    }

    public int getGarbageCollectThresholdMB() {
        return garbageCollectThresholdMB;
    }

    public int getVerticlePort() {
        return verticlePort;
    }
}
