package com.domhauton.membrane.config;

import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dominic on 23/01/17.
 */
class Config {
    private List<String> folders;

    public Config() {
        folders = new ArrayList<>();
    }

    public List<String> getFolders() {
        return folders;
    }

    public void setFolders(List<String> folders) {
        this.folders = folders;
    }

    @Override
    public String toString() {
        return "Config{" +
                "folders=" + folders +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Config config = (Config) o;
        return Objects.equal(folders, config.folders);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(folders);
    }
}
