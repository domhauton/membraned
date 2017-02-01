package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
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
     * @param storedPath file that was add
     * @param modificationDateTime add/modification time
     */
    public void addFile(List<String> shardHash, DateTime modificationDateTime, Path storedPath, OutputStreamWriter outputStreamWriter) throws IOException {
        FileVersion fileVersion = new FileVersion(shardHash, modificationDateTime);

        JournalEntry journalEntry = storageJournal.addEntry(fileVersion, FileOperation.ADD, storedPath, modificationDateTime);
        fileInfoMap.put(storedPath, fileVersion);

        outputStreamWriter.write(journalEntry.toString() + "\n");
    }

    /**
     * Add a remove operation for the path to the catalogue
     * @param storedPath file that was removed
     * @param modificationDateTime removal time
     */
    public void removeFile(Path storedPath, DateTime modificationDateTime) {
        FileVersion fileVersion = new FileVersion(Collections.emptyList(), modificationDateTime);
        storageJournal.addEntry(fileVersion, FileOperation.REMOVE, storedPath, modificationDateTime);
        fileInfoMap.remove(storedPath);
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
                .map(FileVersion::getMD5ShardList)
                .flatMap(List::stream)
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

    public List<String> serializeBaseMap() {
        return baseFileInfoMap.entrySet().stream()
                .map(entry -> CatalogueUtils.serializeEntry(entry.getKey(), entry.getValue().getModificationDateTime(), entry.getValue().getMD5ShardList()))
                .collect(Collectors.toList());
    }
}
