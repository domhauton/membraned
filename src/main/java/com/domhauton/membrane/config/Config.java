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
    private int maxStorageSizeMB;
    private List<WatchFolder> folders;

    public Config() {
        folders = new ArrayList<>();
        shardStorageFolder = System.getProperty("user.home") + File.separator + ".membrane";
        fileRescanFrequencySec = 10;
        folderRescanFrequencySec = 120;
        storageTrimFrequencyMin = 20;
        chunkSizeMB = 64;
        maxStorageSizeMB = 2048;
    }

    public Config(String shardStorageFolder, int fileRescanFrequencySec, int folderRescanFrequencySec, int storageTrimFrequencyMin, int chunkSizeMB, int maxStorageSizeMB, List<WatchFolder> folders) {
        this.shardStorageFolder = shardStorageFolder;
        this.fileRescanFrequencySec = fileRescanFrequencySec;
        this.folderRescanFrequencySec = folderRescanFrequencySec;
        this.storageTrimFrequencyMin = storageTrimFrequencyMin;
        this.chunkSizeMB = chunkSizeMB;
        this.maxStorageSizeMB = maxStorageSizeMB;
        this.folders = folders;
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
}
