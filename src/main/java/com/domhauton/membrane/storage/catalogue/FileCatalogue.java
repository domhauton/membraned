package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import com.google.common.collect.HashMultiset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 */
public class FileCatalogue {
  private final Logger logger;
  private final Map<Path, FileVersion> baseFileInfoMap;
  private final Map<Path, FileVersion> fileInfoMap;
  private final StorageJournal storageJournal;

  FileCatalogue() {
    this(new HashMap<>(), new LinkedList<>());
  }

  public FileCatalogue(Map<Path, FileVersion> baseFileInfoMap, List<JournalEntry> entries) {
    this(baseFileInfoMap, new StorageJournal(entries));
  }

  private FileCatalogue(Map<Path, FileVersion> baseFileInfoMap, StorageJournal storageJournal) {
    logger = LogManager.getLogger();
    this.baseFileInfoMap = baseFileInfoMap;
    this.storageJournal = storageJournal;
    this.fileInfoMap = storageJournal.mapWithJournal(baseFileInfoMap);
  }

  /**
   * Moves the base of the catalogue to the given datetime.
   *
   * @param until move the base till this point
   * @return updated catalogue
   */
  public FileCatalogue cleanCatalogue(DateTime until) {
    StorageJournal oldJournal = storageJournal.getJournalEntriesBeforeTime(until);
    StorageJournal newJournal = storageJournal.getJournalEntriesAfterTime(until);
    Map<Path, FileVersion> newBaseMap = oldJournal.mapWithJournal(baseFileInfoMap);
    Set<Path> oldEntries = newBaseMap.entrySet().stream()
            .filter(x -> x.getValue().getModificationDateTime().isBefore(until))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    oldEntries.forEach(newBaseMap::remove);
    return new FileCatalogue(newBaseMap, newJournal);
  }

  public Map<Path, FileVersion> getCurrentFileMappings() {
    Map<Path, FileVersion> map = new HashMap<>(fileInfoMap.size(), 1.0f);
    map.putAll(fileInfoMap);
    return map;
  }

  /**
   * Reverts catalogue to given time
   */
  FileCatalogue revertTo(DateTime until) {
    StorageJournal newJournal = storageJournal.getJournalEntriesBeforeTime(until);
    return new FileCatalogue(baseFileInfoMap, newJournal);
  }

  /**
   * Add an add operation for the path to the catalogue
   *
   * @param modificationDateTime add/modification time
   * @param storedPath           file that was add
   */
  public synchronized void addFile(List<MD5HashLengthPair> MD5HashLengthPairs, DateTime modificationDateTime, Path storedPath, OutputStreamWriter outputStreamWriter) throws IOException {
    FileVersion newFileVersion = new FileVersion(MD5HashLengthPairs, modificationDateTime);


    // Check if this is an entry before known history.

    FileVersion baseVersionForFile = baseFileInfoMap.get(storedPath);
    if (baseVersionForFile != null && baseVersionForFile.getModificationDateTime().isAfter(modificationDateTime)) {

      // Should be stored anyway for persistence reasons. Will be moved from journal to file next rebase if required.
      storageJournal.addEntry(newFileVersion, FileOperation.ADD, storedPath, modificationDateTime);

      // Hot-swap the baseFile. We now need to slip in the new base file.
      logger.debug("Added file is before the base. Switching the base with this file.");
      baseFileInfoMap.put(storedPath, newFileVersion);
      newFileVersion = baseVersionForFile;
      modificationDateTime = baseVersionForFile.getModificationDateTime();
    }

    JournalEntry journalEntry = storageJournal.addEntry(newFileVersion, FileOperation.ADD, storedPath, modificationDateTime);

    // Check if this actually belongs at the end of the storage journal
    FileVersion fileInfoMapVersion = fileInfoMap.get(storedPath);
    if (fileInfoMapVersion == null || fileInfoMapVersion.getModificationDateTime().isBefore(modificationDateTime)) {
      fileInfoMap.put(storedPath, newFileVersion);
    }

    outputStreamWriter.write(journalEntry.toString() + "\n");
    outputStreamWriter.flush();
  }

  /**
   * Add a new journal entry externally.
   *
   * @param journalEntry journalEntry to add
   * @throws IOException If trouble persisting.
   */
  public synchronized void addJournalEntry(JournalEntry journalEntry, OutputStreamWriter outputStreamWriter) throws IOException {
    boolean entryInCatalogueAlready = getFileVersionHistory(journalEntry.getFilePath()).contains(journalEntry);

    if (!entryInCatalogueAlready) {
      logger.trace("Adding journal entry. [{}]", journalEntry);
      if (journalEntry.getFileOperation() == FileOperation.ADD) {
        addFile(journalEntry.getShardInfo().getMD5HashLengthPairs(), journalEntry.getDateTime(), journalEntry.getFilePath(), outputStreamWriter);
      } else {
        removeFile(journalEntry.getFilePath(), journalEntry.getDateTime(), outputStreamWriter);
      }
    } else {
      logger.trace("Ignoring journal entry as already in catalogue. [{}]", journalEntry);
    }
  }

  /**
   * Add a remove operation for the path to the catalogue
   *
   * @param storedPath           file that was removed
   * @param modificationDateTime removal time
   */
  public synchronized void removeFile(Path storedPath, DateTime modificationDateTime, OutputStreamWriter outputStreamWriter) throws IOException {
    FileVersion newFileVersion = new FileVersion(Collections.emptyList(), modificationDateTime);
    fileInfoMap.remove(storedPath);


    JournalEntry journalEntry = storageJournal.addEntry(newFileVersion, FileOperation.REMOVE, storedPath, modificationDateTime);

    // Check if this actually belongs at the end of the storage journal
    FileVersion fileInfoMapVersion = fileInfoMap.get(storedPath);
    if (fileInfoMapVersion != null && fileInfoMapVersion.getModificationDateTime().isAfter(modificationDateTime)) {
      fileInfoMap.remove(storedPath);
    }

    outputStreamWriter.write(journalEntry.toString() + "\n");
    outputStreamWriter.flush();
  }

  /**
   * Remove any reference to given file
   *
   * @param storedPath file that was removed
   */
  public synchronized void forgetFile(Path storedPath) {
    fileInfoMap.remove(storedPath);
    baseFileInfoMap.remove(storedPath);
    storageJournal.forgetFile(storedPath);
  }

  /**
   * Returns the most recently inserted file version.
   *
   * @param path path of the file to find
   * @return current version of the file
   */
  public Optional<FileVersion> getFileVersion(Path path) {
    return Optional.ofNullable(fileInfoMap.get(path));
  }

  /**
   * Returns the most correct known file version at the time.
   *
   * @param path path of file to find.
   * @return A version of the file at that point in time.
   */
  public Optional<FileVersion> getFileVersion(Path path, DateTime atTime) {
    StorageJournal fullJournal = new StorageJournal(getFileVersionHistory(path));
    StorageJournal trimmedJournal = fullJournal.getJournalEntriesBeforeTime(atTime);
    FileVersion fv = trimmedJournal.mapWithJournal(new HashMap<>(1, 1.0f)).get(path);
    return Optional.ofNullable(fv);
  }

  /**
   * Returns the complete journal for the given file.
   *
   * @param path path of the file
   * @return List of all journal entries related to the file.
   */
  public List<JournalEntry> getFileVersionHistory(Path path) {
    List<JournalEntry> retList = new LinkedList<>();
    FileVersion original = baseFileInfoMap.get(path);
    if (original != null) {
      JournalEntry originalEntry = new JournalEntry(original.getModificationDateTime(), original, FileOperation.ADD, path);
      retList.add(originalEntry);
    }
    retList.addAll(storageJournal.getJournalEntries(path));
    return retList;
  }

  public List<JournalEntry> getFullJournal() {
    return storageJournal.getJournalEntries();
  }

  /**
   * Get all shards referenced in the catalogue
   *
   * @return set of all shard md5 hashes
   */
  public Set<String> getReferencedShards() {
    Set<String> retShards = baseFileInfoMap.values().stream()
            .map(FileVersion::getMD5HashLengthPairs)
            .flatMap(List::stream)
            .map(MD5HashLengthPair::getMd5Hash)
            .collect(Collectors.toSet());
    Set<String> journalShards = storageJournal.getReferencedShards();
    retShards.addAll(journalShards);
    return retShards;
  }

  /**
   * Return all related journal entries
   *
   * @return All entries for the shard.
   */
  public List<JournalEntry> getAllRelatedJournalEntries(String shardId) {
    // First extract any base entries with the information
    List<JournalEntry> relatedEntries = baseFileInfoMap.entrySet().stream()
        .filter(fileVersionEntry -> fileVersionEntry.getValue().getMD5HashList().contains(shardId))
        .map(fv -> new JournalEntry(fv.getValue().getModificationDateTime(), fv.getValue(), FileOperation.ADD, fv.getKey()))
        .collect(Collectors.toList());

    // Add all journal entries.
    relatedEntries.addAll(storageJournal.getJournalEntries(shardId));
    return relatedEntries;
  }

  /**
   * Return all current existing paths
   *
   * @return existing path set
   */
  public Set<Path> getCurrentFiles() {
    return fileInfoMap.keySet();
  }

  /**
   * Return all files referenced in the catalogue
   *
   * @return referenced file path set
   */
  public Set<Path> getReferencedFiles() {
    Set<Path> baseReferencedFiles = new HashSet<>(baseFileInfoMap.keySet());
    baseReferencedFiles.addAll(storageJournal.getReferencedPaths());
    return baseReferencedFiles;
  }

  public DateTime getOldestJournalEntryTime() {
    return storageJournal.getEarliestDateTime();
  }

  public List<String> serializeBaseMap() {
    return baseFileInfoMap.entrySet().stream()
            .map(entry -> CatalogueUtils.serializeEntry(entry.getKey(), entry.getValue().getModificationDateTime(), entry.getValue().getMD5HashLengthPairs()))
            .collect(Collectors.toList());
  }

  public synchronized long removeOldestJournalEntries(int bytesToRemove) {
    List<String> journalHashList = storageJournal.getJournalEntries().stream()
            .map(JournalEntry::getShardInfo)
            .map(FileVersion::getMD5HashLengthPairs)
            .flatMap(List::stream)
            .map(MD5HashLengthPair::getMd5Hash)
            .collect(Collectors.toList());
    List<String> baseHashList = baseFileInfoMap.values().stream()
            .map(FileVersion::getMD5HashLengthPairs)
            .flatMap(List::stream)
            .map(MD5HashLengthPair::getMd5Hash)
            .collect(Collectors.toList());

    HashMultiset<String> shardCounts = HashMultiset.create(journalHashList);
    shardCounts.addAll(baseHashList);

    int removedByteCount = 0;
    for (JournalEntry journalEntry : storageJournal.getJournalEntries()) {
      if (removedByteCount < bytesToRemove) {
        FileVersion latestFileVersion = fileInfoMap.get(journalEntry.getFilePath());
        boolean isLatestEntry = latestFileVersion != null && latestFileVersion.equals(journalEntry.getShardInfo());
        if (!isLatestEntry) {
          logger.debug("Removing old journal entry: {}", journalEntry::toString);
          storageJournal.forgetEntry(journalEntry);
          removedByteCount += journalEntry.getShardInfo().getMD5HashLengthPairs().stream()
                  .filter(x -> shardCounts.remove(x.getMd5Hash(), 1) <= 1)
                  .mapToInt(MD5HashLengthPair::getLength)
                  .sum();
        }
      } else {
        break;
      }
      if (logger.isTraceEnabled()) {
        logger.trace("Removing old journal entry. Currently removed {}MB", ((float) removedByteCount) / (1024 * 1024));
      }
    }
    return removedByteCount;
  }
}
