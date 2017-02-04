package com.domhauton.membrane.view.responses;

import com.domhauton.membrane.config.items.WatchFolder;

import java.util.List;
import java.util.Set;

/**
 * Created by dominic on 03/02/17.
 */
public class FileManagerStatus {
    private Set<String> trackedFolders;
    private Set<String> trackedFiles;

    public FileManagerStatus(Set<String> trackedFolders, Set<String> trackedFiles) {
        this.trackedFolders = trackedFolders;
        this.trackedFiles = trackedFiles;
    }

    public Set<String> getTrackedFolders() {
        return trackedFolders;
    }

    public Set<String> getTrackedFiles() {
        return trackedFiles;
    }
}
