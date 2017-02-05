package com.domhauton.membrane.restful.responses;

import java.util.List;

/**
 * Created by dominic on 05/02/17.
 */
public class FileHistoryEntry {
    private long dateTimeMillis;
    private List<String> hashes;
    private long size;
    private boolean isRemove;

    public FileHistoryEntry(long dateTimeMillis, List<String> hashes, long size, boolean isRemove) {
        this.dateTimeMillis = dateTimeMillis;
        this.hashes = hashes;
        this.size = size;
        this.isRemove = isRemove;
    }

    public long getDateTime() {
        return dateTimeMillis;
    }

    public List<String> getMD5Hashes() {
        return hashes;
    }

    public long getSize() {
        return size;
    }

    public boolean isRemove() {
        return isRemove;
    }
}
