package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.metadata.FileOperation;
import com.domhauton.membrane.storage.metadata.FileVersion;
import com.domhauton.membrane.storage.shard.ShardStorageImpl;
import org.joda.time.DateTime;
import sun.awt.image.ImageWatched;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 */
public class FileCatalogue {
    Map<Path, FileVersion> baseFileInfoMap;
    Map<Path, FileVersion> fileInfoMap;
    StorageJournal storageJournal;
    ShardStorageImpl shardStorage;

    public FileCatalogue() {
        this(new HashMap<>());
    }

    public FileCatalogue(Map<Path, FileVersion> baseFileInfoMap) {
        this(baseFileInfoMap, Collections.emptyList());
    }

    private FileCatalogue(Map<Path, FileVersion> baseFileInfoMap, List<JournalEntry> journalEntries) {
        this.baseFileInfoMap = baseFileInfoMap;
        this.fileInfoMap = new HashMap<>();
        fileInfoMap.putAll(baseFileInfoMap);
        storageJournal = new StorageJournal(journalEntries);
        journalEntries.forEach(this::addJournalEntry);
    }

    public void collapseJournal() {
        baseFileInfoMap = fileInfoMap;
        fileInfoMap = new HashMap<>();
        storageJournal = new StorageJournal();
    }

    public FileCatalogue getCatalogueAtTime(DateTime until) {
        return new FileCatalogue(baseFileInfoMap, storageJournal.getJournalEntries(until));
    }

    public void addFile(List<String> shardHash, DateTime modificationDateTime, Path storedPath) {
        FileVersion fileVersion = new FileVersion(shardHash, modificationDateTime, storedPath);
        storageJournal.addEntry(fileVersion, FileOperation.ADD, storedPath);
        fileInfoMap.put(storedPath, fileVersion);
    }

    public void removeFile(Path storedPath, DateTime modificationDateTime) {
        FileVersion fileVersion = new FileVersion(Collections.emptyList(), modificationDateTime, storedPath);
        storageJournal.addEntry(fileVersion, FileOperation.REMOVE, storedPath);
        fileInfoMap.remove(storedPath);
    }

    private void addJournalEntry(JournalEntry journalEntry) {
        switch(journalEntry.getFileOperation()) {
            case ADD:
                fileInfoMap.put(journalEntry.getFilePath(), journalEntry.getShardInfo());
                break;
            case REMOVE:
            default:
                fileInfoMap.remove(journalEntry.getFilePath());
        }
    }
}
