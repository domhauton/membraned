package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.catalogue.CatalogueUtils;
import com.domhauton.membrane.storage.catalogue.FileCatalogue;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.domhauton.membrane.storage.shard.ShardStorage;
import com.domhauton.membrane.storage.shard.ShardStorageException;
import com.domhauton.membrane.storage.shard.ShardStorageImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 */
public class StorageManager {

    private static final String DEFAULT_BASE_PATH = System.getProperty("user.home") + File.separator + ".membrane";
    static final String DEFAULT_STORAGE_FOLDER = "data";
    static final String DEFAULT_CATALOGUE_FOLDER = "catalogue";
    static final String JOURNAL_NAME = "journal.csv";
    static final String BASE_FILE_MAP_NAME = "file-map.csv";

    private Logger logger;
    private ShardStorage shardStorage;
    private FileCatalogue fileCatalogue;
    private Set<String> tempProtectedShards;

    private Path baseFileMapPath;
    private Path journalPath;

    private OutputStreamWriter journalOutput;

    public StorageManager() throws StorageManagerException {
        this(Paths.get(DEFAULT_BASE_PATH));
    }

    public StorageManager(Path basePath) throws StorageManagerException {
        this(
                Paths.get(basePath.toString() + File.separator + DEFAULT_CATALOGUE_FOLDER),
                Paths.get(basePath.toString() + File.separator + DEFAULT_STORAGE_FOLDER));
    }

    public StorageManager(Path catalogue, Path data) throws StorageManagerException {
        logger = LogManager.getLogger();
        shardStorage = new ShardStorageImpl(data);
        tempProtectedShards = new HashSet<>();
        journalPath = Paths.get(catalogue.toString() + File.separator + JOURNAL_NAME);
        List<JournalEntry> journalEntries = journalPath.toFile().exists() ? readJournal(journalPath) : new LinkedList<>();
        baseFileMapPath = Paths.get(catalogue.toString() + File.separator + BASE_FILE_MAP_NAME);
        Map<Path, FileVersion> fileMap = baseFileMapPath.toFile().exists() ? readFileMap(baseFileMapPath) : new HashMap<>();
        journalOutput = openJournalOutputStream(journalPath);
        fileCatalogue = new FileCatalogue(fileMap, journalEntries);
    }

    /**
     * Persist the given shard in the storage manager
     * @param md5Hash the MD5 has of the data.
     * @param data The data to persist
     * @throws StorageManagerException If there is a problem writing the data.
     */
    public void storeShard(String md5Hash, byte[] data) throws StorageManagerException {
        try {
            logger.info("Storing shard [{}]", md5Hash);
            tempProtectedShards.add(md5Hash);
            shardStorage.storeShard(md5Hash, data);
        } catch (ShardStorageException e) {
            throw new StorageManagerException(e.getMessage());
        }
    }

    /**
     * Add a file to be stored
     * @param shardHash All of the file's shards in order of reconstruction
     * @param modificationDateTime Time the modification occurred.
     * @param storedPath The actual path of the file.
     */
    public synchronized void addFile(List<String> shardHash, DateTime modificationDateTime, Path storedPath) throws StorageManagerException {
        logger.info("Adding file [{}]. Timestamp [{}]. Shards [{}]", storedPath, modificationDateTime, shardHash);
        try {
            fileCatalogue.addFile(shardHash, modificationDateTime, storedPath, journalOutput);
        } catch (IOException e) {
            throw new StorageManagerException("Failed to write update to journal.");
        }
    }

    /**
     * Indicates the file was removed.
     * @param storedPath The path of the stored file
     * @param modificationDateTime The time the removal occurred.
     */
    public void removeFile(Path storedPath, DateTime modificationDateTime) {
        logger.info("Removing file [{}] at {}", storedPath, modificationDateTime);
        fileCatalogue.removeFile(storedPath, modificationDateTime);
    }

    /**
     * Rebuilds the given file at the given destination.
     * @param originalPath file to recover
     * @param destPath where to write recovered file to
     * @throws StorageManagerException If there's a problem reading the shards, finding metadata or writing result.
     */
    public void rebuildFile(Path originalPath, Path destPath) throws StorageManagerException {
        logger.info("Rebuilding file [{}]. Destination [{}]", originalPath, destPath);
        Optional<FileVersion> fileVersionOptional = fileCatalogue.getFileVersion(originalPath);
        if (fileVersionOptional.isPresent()) {
            FileVersion fileVersion = fileVersionOptional.get();
            try (
                    FileOutputStream fos = new FileOutputStream(destPath.toFile());
                    BufferedOutputStream bus = new BufferedOutputStream(fos)
            ) {
                for (String md5Hash : fileVersion.getMD5ShardList()) {
                    logger.info("Rebuilding file [{}]. Shard [{}]", originalPath, md5Hash);
                    byte[] data = shardStorage.retrieveShard(md5Hash);
                    bus.write(data);
                }
                logger.info("Rebuilding file [{}]. Complete. SUCCESS.", originalPath);
            } catch (ShardStorageException e) {
                try {
                    Files.delete(destPath);
                } catch (IOException e1) {
                    logger.error("Failed to remove partially reconstructed file.");
                }
                throw new StorageManagerException("Shard missing. Reconstruction failed. " + e.getMessage());
            } catch (FileNotFoundException e) {
                throw new StorageManagerException("Could not find output file. " + e.getMessage());
            } catch (IOException e) {
                throw new StorageManagerException("Could not write to output file. " + e.getMessage());
            }
        } else {
            throw new StorageManagerException("File unknown. [" + originalPath + "]");
        }
    }

    /**
     * Removes any shards not referenced in the catalogue from the storage.
     */
    public void collectGarbage() {
        Set<String> requiredShards = fileCatalogue.getReferencedShards();
        Set<String> garbageShards = shardStorage.listShards();
        garbageShards.removeAll(requiredShards);
        garbageShards.removeAll(tempProtectedShards);
        garbageShards.forEach(x -> {
            try {
                shardStorage.removeShard(x);
            } catch (ShardStorageException e) {
                // Ignore - It doesn't exist already
            }
        });
    }

    public synchronized void cleanStorage(DateTime moveTo) throws StorageManagerException {
        fileCatalogue = fileCatalogue.cleanCatalogue(moveTo);
        String dtString = DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMdd-HHmmss"));
        try{
            logger.info("Closing journal temporarily. [{}}", journalPath);
            journalOutput.close();

            Path backupJournalPath = Paths.get(journalPath.toString() + ".bkp." + dtString);
            logger.info("Journal backup to [{}]", backupJournalPath);
            Files.deleteIfExists(backupJournalPath);
            Files.move(journalPath, backupJournalPath);

            Path baseFileMapBackupPath = Paths.get(baseFileMapPath.toString() + ".bkp." + dtString);
            logger.info("Base File Map backup to [{}]", baseFileMapBackupPath);
            Files.deleteIfExists(baseFileMapBackupPath);
            Files.move(baseFileMapPath, baseFileMapBackupPath);

            List<String> newJournalEntries = fileCatalogue.getFullJournal().stream()
                    .map(JournalEntry::toString)
                    .map(s -> s + "\n")
                    .collect(Collectors.toList());

            logger.info("Writing new journal to [{}]", journalPath);
            Files.write(journalPath, newJournalEntries);
            logger.info("Writing new base file map to [{}]", journalPath);
            Files.write(baseFileMapPath, fileCatalogue.serializeBaseMap());
            logger.info("Reopening journal. [{}]", journalPath);
            journalOutput = openJournalOutputStream(journalPath);
        } catch (IOException e) {
            logger.error("There was an IOException while cleaning storage! Might need to manually recover!");
            throw new StorageManagerException("IOException while cleaning storage: " + e.toString());
        }
        logger.info("Running garbage collection for de-referenced shards.");
        collectGarbage();
    }

    public void clearProtectedShards() {
        tempProtectedShards.clear();
    }

    public Map<Path, FileVersion> getCurrentFileMapping() {
        return fileCatalogue.getCurrentFileMappings();
    }

    private List<JournalEntry> readJournal(Path journalPath) throws StorageManagerException {
        try {
            List<String> journalEntries = Files.readAllLines(journalPath);
            List<JournalEntry> convertedJournalEntries = new LinkedList<>();
            for(String fileEntry : journalEntries) {
                logger.debug("Parsing journal: {}", fileEntry);
                JournalEntry entry = new JournalEntry(fileEntry);
                convertedJournalEntries.add(entry);
            }
            logger.info("Loaded {} entries from journal at [{}]", convertedJournalEntries.size(), journalPath);
            return convertedJournalEntries;
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Could not read from journal at {}", journalPath);
            throw new StorageManagerException("Could not read journal");
        }
    }

    private Map<Path, FileVersion> readFileMap(Path fileMapPath) throws StorageManagerException {
        try {
            List<String> fileMapEntries = Files.readAllLines(fileMapPath);
            Map<Path, FileVersion> retMap = CatalogueUtils.generateInputMap(fileMapEntries);
            logger.info("Loaded {} entries from file map at [{}]", retMap.size(), fileMapPath);
            return retMap;
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Could not read from journal at {}", fileMapPath);
            throw new StorageManagerException("Could not read filemap: " + fileMapPath);
        }
    }

    private OutputStreamWriter openJournalOutputStream(Path journalPath) throws StorageManagerException {
        try {
            if(!journalPath.toFile().exists()) {
                boolean created = journalPath.toFile().getParentFile().mkdirs();
                logger.debug("Created dirs for journal: {}", created);
                Files.createFile(journalPath);
            }
            return new OutputStreamWriter(new FileOutputStream(journalPath.toFile()));
        } catch (IOException e) {
            logger.error("Could not open journal for writing at {}", journalPath);
            throw new StorageManagerException("Could not open journal for writing. " + journalPath);
        }
    }

    public synchronized void close() throws StorageManagerException {
        logger.info("Closing storage manager.");
        try {
            journalOutput.close();
        } catch (IOException e) {
            logger.error("Could not flush journal output!");
            throw new StorageManagerException("Could not flush journal output!");
        }
    }
}
