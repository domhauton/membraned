package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.metadata.FileVersion;
import com.domhauton.membrane.storage.metadata.FileOperation;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.Comparator;

/**
 * Created by dominic on 30/01/17.
 */
public class JournalEntry {
    private final DateTime dateTime;
    private final FileVersion shardInfo;
    private final FileOperation fileOperation;
    private final Path filePath;

    public JournalEntry(DateTime dateTime, FileVersion shardInfo, FileOperation fileOperation, Path filePath) {
        this.dateTime = dateTime;
        this.shardInfo = shardInfo;
        this.fileOperation = fileOperation;
        this.filePath = filePath;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public FileVersion getShardInfo() {
        return shardInfo;
    }

    public FileOperation getFileOperation() {
        return fileOperation;
    }

    public Path getFilePath() {
        return filePath;
    }

    public static Comparator<JournalEntry> getComparator() {
        return Comparator.comparing(JournalEntry::getDateTime);
    }
}
