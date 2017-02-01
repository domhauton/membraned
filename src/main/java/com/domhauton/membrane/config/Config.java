package com.domhauton.membrane.config;

import com.domhauton.membrane.config.items.WatchFolder;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dominic on 23/01/17.
 */
public class Config {
    private List<WatchFolder> folders;

    public Config() {
        folders = new ArrayList<>();
    }

    public List<WatchFolder> getFolders() {
        return folders;
    }
}
