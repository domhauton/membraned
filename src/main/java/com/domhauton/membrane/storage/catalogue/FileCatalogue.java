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
    private Logger logger;
    private Map<Path, FileVersion> baseFileInfoMap;
    private Map<Path, FileVersion> fileInfoMap;
    private StorageJournal storageJournal;

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
    public FileCatalogue revertTo(DateTime until) {
        StorageJournal newJournal = storageJournal.getJournalEntriesBeforeTime(until);
        return new FileCatalogue(baseFileInfoMap, newJournal);
    }

    /**
     * Add an add operation for the path to the catalogue
     * @param MD5HashLengthPairs
     * @param modificationDateTime add/modification time
     * @param storedPath file that was add
     */
    public synchronized void addFile(List<MD5HashLengthPair> MD5HashLengthPairs, DateTime modificationDateTime, Path storedPath, OutputStreamWriter outputStreamWriter) throws IOException {
        FileVersion fileVersion = new FileVersion(MD5HashLengthPairs, modificationDateTime);

        JournalEntry journalEntry = storageJournal.addEntry(fileVersion, FileOperation.ADD, storedPath, modificationDateTime);
        fileInfoMap.put(storedPath, fileVersion);

        outputStreamWriter.write(journalEntry.toString() + "\n");
    }

    /**
     * Add a remove operation for the path to the catalogue
     * @param storedPath file that was removed
     * @param modificationDateTime removal time
     */
    public synchronized void removeFile(Path storedPath, DateTime modificationDateTime) {
        FileVersion fileVersion = new FileVersion(Collections.emptyList(), modificationDateTime);
        storageJournal.addEntry(fileVersion, FileOperation.REMOVE, storedPath, modificationDateTime);
        fileInfoMap.remove(storedPath);
    }

    /**
     * Remove any reference to given file
     * @param storedPath file that was removed
     */
    public synchronized void forgetFile(Path storedPath) {
        fileInfoMap.remove(storedPath);
        baseFileInfoMap.remove(storedPath);
        storageJournal.forgetFile(storedPath);
    }

    /**
     * Returns the most recently inserted file version.
     * @param path path of the file to find
     * @return current version of the file
     */
    public Optional<FileVersion> getFileVersion(Path path) {
        return Optional.ofNullable(fileInfoMap.get(path));
    }

    /**
     * Returns the most correct known file version at the time.
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
     * @param path path of the file
     * @return List of all journal entries related to the file.
     */
    public List<JournalEntry> getFileVersionHistory(Path path) {
        List<JournalEntry> retList = new LinkedList<>();
        FileVersion original = baseFileInfoMap.get(path);
        if(original != null) {
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
     * Return all current existing paths
     * @return existing path set
     */
    public Set<Path> getCurrentFiles() {
        return fileInfoMap.keySet();
    }

    /**
     * Return all files referenced in the catalogue
     * @return referenced file path set
     */
    public Set<Path> getReferencedFiles() {
        Set<Path> baseReferencedFiles = baseFileInfoMap.keySet();
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
        for(JournalEntry journalEntry : storageJournal.getJournalEntries()) {
            if(removedByteCount < bytesToRemove) {
                FileVersion latestFileVersion = fileInfoMap.get(journalEntry.getFilePath());
                boolean isLatestEntry = latestFileVersion != null && latestFileVersion.equals(journalEntry.getShardInfo());
                if(!isLatestEntry) {
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
            if(logger.isTraceEnabled()) {
                logger.trace("Removing old journal entry. Currently removed {}MB", ((float)removedByteCount) / (1024*1024));
            }
        }
        return removedByteCount;
    }
}
