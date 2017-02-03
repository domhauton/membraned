package com.domhauton.membrane.storage.catalogue.metadata;

import com.google.common.base.Objects;

/**
 * Created by dominic on 03/02/17.
 */
public class MD5HashLengthPair {
    private final String md5Hash;
    private final Integer length;

    public MD5HashLengthPair(String md5Hash, Integer length) {
        this.md5Hash = md5Hash;
        this.length = length;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public Integer getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "MD5HashLengthPair{" +
                "md5Hash='" + md5Hash + '\'' +
                ", length=" + length +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MD5HashLengthPair that = (MD5HashLengthPair) o;
        return Objects.equal(md5Hash, that.md5Hash) &&
                Objects.equal(length, that.length);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(md5Hash, length);
    }
}
