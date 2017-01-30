package com.domhauton.membrane.storage.journal;

import com.domhauton.membrane.storage.metadata.FileVersion;
import com.domhauton.membrane.storage.metadata.FileOperation;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by dominic on 30/01/17.
 */
public class StorageJournal {
    private LinkedList<JournalEntry> journalEntries;

    public StorageJournal() {
        journalEntries = new LinkedList<>();
    }

    /**
     * Adds a new modification to the journal at this point.
     */
    public void addEntry(FileVersion shardInfo, FileOperation fileOperation, Path filePath) {
        JournalEntry journalEntry = new JournalEntry(DateTime.now(), shardInfo, fileOperation, filePath);
        addEntry(journalEntry);
        // TODO: Consider persisting.
    }

    /**
     * Adds a new point to the given journal
     */
    private void addEntry(JournalEntry journalEntry) {
        journalEntries.add(journalEntry);
    }

    /**
     * Reverts the given fileInfoMap to the state at given datetime
     * @param fileInfoMap The map to apply the state to. Should be the head of the journal.
     * @param revertToTime Only revert updates after this point.
     */
    public void revertTo(Map<Path, FileInfo> fileInfoMap, DateTime revertToTime) {
        journalEntries.stream()
                .filter(journalEntry -> journalEntry.getDateTime().isAfter(revertToTime))
                .sorted(JournalEntry.getComparator().reversed())
                .forEachOrdered(entry -> fileInfoMap.get(entry.getFilePath())
                        .modifyFileShards(entry.getFileOperation().reverse(), entry.getShardInfo()));
    }
}
