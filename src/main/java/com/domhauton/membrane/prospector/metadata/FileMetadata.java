package com.domhauton.membrane.prospector.metadata;

import com.google.common.base.Objects;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by dominic on 26/01/17.
 */
public class FileMetadata {
    private final String fullPath;
    private final DateTime modifiedTime;
    private final DateTime accessTime;
    private final List<String> md5HashList;

    FileMetadata(
            String fullPath,
            DateTime modifiedTime,
            DateTime accessTime,
            List<String> md5HashList) {
        this.fullPath = fullPath;
        this.modifiedTime = modifiedTime;
        this.accessTime = accessTime;
        this.md5HashList = md5HashList;
    }

    public String getFullPath() {
        return fullPath;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public List<String> getMD5HashList() {
        return md5HashList;
    }


    public synchronized boolean hashCodeEqual(FileMetadata that) {
        if(that == null) {
            return false;
        }
        return that.getMD5HashList().equals(this.getMD5HashList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMetadata that = (FileMetadata) o;
        return Objects.equal(fullPath, that.fullPath) &&
                Objects.equal(accessTime, that.accessTime) &&
                Objects.equal(md5HashList, that.md5HashList);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullPath, accessTime, md5HashList);
    }
}
