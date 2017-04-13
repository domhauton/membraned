package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 */
class StorageJournal {
  private final Logger logger;
  private final List<JournalEntry> journalEntries;

  StorageJournal(List<JournalEntry> journalEntries) {
    this.journalEntries = journalEntries;
    logger = LogManager.getLogger();
  }

  /**
   * Adds a new modification to the journal at this point.
   */
  synchronized JournalEntry addEntry(FileVersion shardInfo, FileOperation fileOperation, Path filePath, DateTime dateTime) {
    JournalEntry journalEntry = new JournalEntry(dateTime, shardInfo, fileOperation, filePath);
    addEntry(journalEntry);
    return journalEntry;
  }

  private synchronized void addEntry(JournalEntry journalEntry) {
    logger.trace("Adding entry to log {} {} {}", journalEntry.getFilePath(), journalEntry.getFileOperation(), journalEntry.getDateTime());
    journalEntries.add(journalEntry);
  }

  synchronized List<JournalEntry> getJournalEntries() {
    return journalEntries.stream()
            .sorted(JournalEntry.getComparator())
            .collect(Collectors.toList());
  }

  synchronized List<JournalEntry> getJournalEntries(String shardId) {
    return journalEntries.stream()
        .filter(entry -> entry.getShardInfo().getMD5HashList().contains(shardId))
        .sorted(JournalEntry.getComparator())
        .collect(Collectors.toList());
  }

  synchronized List<JournalEntry> getJournalEntries(Path path) {
    return journalEntries.stream()
            .filter(entry -> entry.getFilePath().equals(path))
            .sorted(JournalEntry.getComparator())
            .collect(Collectors.toList());
  }

  synchronized StorageJournal getJournalEntriesBeforeTime(DateTime until) {
    List<JournalEntry> newEntries = journalEntries.stream()
            .filter(journalEntry -> !journalEntry.getDateTime().isAfter(until))
            .sorted(JournalEntry.getComparator())
            .collect(Collectors.toList());
    return new StorageJournal(newEntries);
  }

  synchronized StorageJournal getJournalEntriesAfterTime(DateTime startAt) {
    List<JournalEntry> newEntries = journalEntries.stream()
            .filter(journalEntry -> journalEntry.getDateTime().isAfter(startAt))
            .sorted(JournalEntry.getComparator())
            .collect(Collectors.toList());
    return new StorageJournal(newEntries);
  }

  synchronized Map<Path, FileVersion> mapWithJournal(Map<Path, FileVersion> map) {
    Map<Path, FileVersion> newMap = new HashMap<>();
    newMap.putAll(map);
    getJournalEntries().forEach(entry -> applyJournalEntry(newMap, entry));
    return newMap;
  }

  Set<String> getReferencedShards() {
    return journalEntries.stream()
            .map(JournalEntry::getShardInfo)
            .map(FileVersion::getMD5HashLengthPairs)
            .flatMap(List::stream)
            .map(MD5HashLengthPair::getMd5Hash)
            .collect(Collectors.toSet());
  }

  Set<Path> getReferencedPaths() {
    return journalEntries.stream()
            .map(JournalEntry::getFilePath)
            .collect(Collectors.toSet());
  }

  DateTime getEarliestDateTime() {
    return new DateTime(journalEntries.stream()
            .mapToLong(journalEntry -> journalEntry.getDateTime().getMillis())
            .min()
            .orElse(System.currentTimeMillis()));
  }

  synchronized void forgetFile(Path filePath) {
    List<JournalEntry> entriesToForget = getJournalEntries(filePath);
    journalEntries.removeAll(entriesToForget);
  }

  synchronized void forgetEntry(JournalEntry journalEntry) {
    journalEntries.remove(journalEntry);
  }

  private synchronized void applyJournalEntry(Map<Path, FileVersion> map, JournalEntry journalEntry) {
    switch (journalEntry.getFileOperation()) {
      case ADD:
        map.put(journalEntry.getFilePath(), journalEntry.getShardInfo());
        break;
      case REMOVE:
      default:
        map.remove(journalEntry.getFilePath());
    }
  }
}
