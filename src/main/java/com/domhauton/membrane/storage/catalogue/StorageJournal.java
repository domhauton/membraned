package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.metadata.FileVersion;
import com.domhauton.membrane.storage.metadata.FileOperation;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 */
public class StorageJournal {
    private List<JournalEntry> journalEntries;

    public StorageJournal() {
        journalEntries = new LinkedList<>();
    }

    public StorageJournal(List<JournalEntry> journalEntries) {
        this.journalEntries = journalEntries;
    }

    /**
     * Adds a new modification to the journal at this point.
     */
    public void addEntry(FileVersion shardInfo, FileOperation fileOperation, Path filePath) {
        JournalEntry journalEntry = new JournalEntry(DateTime.now(), shardInfo, fileOperation, filePath);
        addEntry(journalEntry);
        // TODO: Consider persisting.
    }

    private void addEntry(JournalEntry journalEntry) {
        journalEntries.add(journalEntry);
    }

    public List<JournalEntry> getJournalEntries() {
        return journalEntries;
    }

    public List<JournalEntry> getJournalEntries(DateTime until) {
        return getJournalEntries().stream()
                .filter(journalEntry -> journalEntry.getDateTime().isBefore(until))
                .sorted(JournalEntry.getComparator()).collect(Collectors.toList());
    }
}
