package com.domhauton.membrane.prospector;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dominic on 31/01/17.
 */
public class ProspectorChangeSet {
    private Set<Path> changedFiles;
    private Set<Path> removedFiles;
    private boolean overflow;

    public ProspectorChangeSet() {
        changedFiles = new HashSet<>();
        removedFiles = new HashSet<>();
        overflow = false;
    }

    public void addChange(Path path) {
        changedFiles.add(path);
    }

    public void addRemoval(Path path) {
        removedFiles.add(path);
    }

    public Set<Path> getChangedFiles() {
        return changedFiles;
    }

    public Set<Path> getRemovedFiles() {
        return removedFiles;
    }

    public void setOverflow() {
        overflow = true;
    }

    public boolean hasOverflown() {
        return overflow;
    }
}
