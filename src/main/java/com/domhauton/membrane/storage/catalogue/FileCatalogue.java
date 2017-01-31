package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.metadata.FileOperation;
import com.domhauton.membrane.storage.metadata.FileVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

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

    public FileCatalogue() {
        this(new HashMap<>());
    }

    public FileCatalogue(Map<Path, FileVersion> baseFileInfoMap) {
        this(baseFileInfoMap, new StorageJournal(new LinkedList<>()));
    }

    private FileCatalogue(Map<Path, FileVersion> baseFileInfoMap, StorageJournal storageJournal) {
        logger = LogManager.getLogger();
        this.baseFileInfoMap = baseFileInfoMap;
        this.storageJournal = storageJournal;
        this.fileInfoMap = storageJournal.mapWithJournal(baseFileInfoMap);
    }

    public FileCatalogue collapseJournal(DateTime until) {
        StorageJournal oldJournal = storageJournal.getJournalEntriesBeforeTime(until);
        StorageJournal newJournal = storageJournal.getJournalEntriesAfterTime(until);
        Map<Path, FileVersion> newBaseMap = oldJournal.mapWithJournal(baseFileInfoMap);
        return new FileCatalogue(newBaseMap, newJournal);
    }

    public FileCatalogue getCatalogueAtTime(DateTime until) {
        StorageJournal newJournal = storageJournal.getJournalEntriesBeforeTime(until);
        return new FileCatalogue(baseFileInfoMap, newJournal);
    }

    public void addFile(List<String> shardHash, DateTime modificationDateTime, Path storedPath) {
        FileVersion fileVersion = new FileVersion(shardHash, modificationDateTime, storedPath);
        storageJournal.addEntry(fileVersion, FileOperation.ADD, storedPath, modificationDateTime);
        fileInfoMap.put(storedPath, fileVersion);
    }

    public void removeFile(Path storedPath, DateTime modificationDateTime) {
        FileVersion fileVersion = new FileVersion(Collections.emptyList(), modificationDateTime, storedPath);
        storageJournal.addEntry(fileVersion, FileOperation.REMOVE, storedPath, modificationDateTime);
        fileInfoMap.remove(storedPath);
    }

    public Optional<FileVersion> getFileVersion(Path path) {
        return Optional.ofNullable(fileInfoMap.get(path));
    }

    public Set<String> getReferencedShards() {
        Set<String> retShards = baseFileInfoMap.values().stream()
                .map(FileVersion::getShardHash)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        Set<String> journalShards = storageJournal.getReferencedShards();
        retShards.addAll(journalShards);
        return retShards;
    }
}
