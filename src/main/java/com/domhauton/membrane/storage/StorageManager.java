package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.catalogue.FileCatalogue;
import com.domhauton.membrane.storage.metadata.FileVersion;
import com.domhauton.membrane.storage.shard.ShardStorageException;
import com.domhauton.membrane.storage.shard.ShardStorageImpl;
import com.google.common.hash.HashCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Created by dominic on 30/01/17.
 */
public class StorageManager {
    private Logger logger;
    private ShardStorageImpl shardStorage;
    private FileCatalogue fileCatalogue;
    private Set<String> tempProtectedShards;

    public StorageManager() {
        logger = LogManager.getLogger();
        shardStorage = new ShardStorageImpl();
        fileCatalogue = new FileCatalogue();
        tempProtectedShards = new HashSet<>();
    }

    /**
     * Persist the given shard in the storage manager
     * @param md5Hash the MD5 has of the data.
     * @param data The data to persist
     * @throws StorageManagerException If there is a problem writing the data.
     */
    public void storeShard(String md5Hash, byte[] data) throws StorageManagerException {
        try {
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
    public void addFile(List<String> shardHash, DateTime modificationDateTime, Path storedPath) {
        fileCatalogue.addFile(shardHash, modificationDateTime, storedPath);
    }

    /**
     * Indicates the file was removed.
     * @param storedPath The path of the stored file
     * @param modificationDateTime The time the removal occurred.
     */
    public void removeFile(Path storedPath, DateTime modificationDateTime) {
        fileCatalogue.removeFile(storedPath, modificationDateTime);
    }

    /**
     * Rebuilds the given file at the given destination.
     * @param originalPath file to recover
     * @param destPath where to write recovered file to
     * @throws StorageManagerException If there's a problem reading the shards, finding metadata or writing result.
     */
    public void rebuildFile(Path originalPath, Path destPath) throws StorageManagerException {
        Optional<FileVersion> fileVersionOptional = fileCatalogue.getFileVersion(originalPath);
        if (fileVersionOptional.isPresent()) {
            FileVersion fileVersion = fileVersionOptional.get();
            try (
                    FileOutputStream fos = new FileOutputStream(destPath.toFile());
                    BufferedOutputStream bus = new BufferedOutputStream(fos)
            ) {
                for (String md5Hash : fileVersion.getShardHash()) {
                    byte[] data = shardStorage.retrieveShard(md5Hash);
                    bus.write(data);
                }
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
}
