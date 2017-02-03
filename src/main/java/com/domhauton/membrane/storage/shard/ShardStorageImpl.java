package com.domhauton.membrane.storage.shard;

import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 */
public class ShardStorageImpl implements ShardStorage {
    private static final String FILE_EXTENSION = ".mem";
    private static final int FOLDER_SPLIT_LEN = 5;

    private Logger logger;
    private Path basePath;
    private long currentStorageSize;
    private final long maxStorageSize;

    /**
     * @param basePath Directory to store shards.
     */
    public ShardStorageImpl(Path basePath, long maxStorageSize) {
        this.basePath = basePath;
        logger = LogManager.getLogger();
        this.maxStorageSize = maxStorageSize;
        currentStorageSize = getStorageSize();
    }

    /**
     * Store the data given under the given hash.
     * @param md5Hash The md5Hash of the data given
     * @param data The data to store.
     * @throws ShardStorageException
     */
    public void storeShard(String md5Hash, byte[] data) throws ShardStorageException {
        Path filePath = getPath(basePath.toString(), md5Hash);
        if(!filePath.toFile().exists()) {
            if(currentStorageSize + data.length > maxStorageSize) {
                currentStorageSize = getStorageSize(); // Refresh to ensure value is correct
                if(currentStorageSize + data.length > maxStorageSize) {
                    logger.warn("Not enough space to store shard [{}] of size {}MB. {}MB of {}MB stored",
                            md5Hash,
                            ((float)data.length)/(1024*1024),
                            ((float)currentStorageSize)/(1024*1024),
                            ((float)maxStorageSize)/(1024*1024));
                    throw new ShardStorageException("Not enough space to store shard. " + md5Hash);
                }
            }
            try {
                boolean success = filePath.toFile().getParentFile().mkdirs();
                logger.debug("Created directories for [{}]: {}", filePath, success);
                Files.write(filePath, data);
                currentStorageSize += data.length;
            } catch (IOException e) {
                logger.error("Could not store shard [{}]. {}", md5Hash, e.getMessage());
                throw new ShardStorageException("Could not store shard [" + md5Hash + "]. " + e.getMessage());
            }
        }
    }

    /**
     * Retrieves a shard from the directory and checks consistency with hash
     * @param md5Hash md5Hash of requested shard
     * @return data requested.
     * @throws ShardStorageException If cannot access shard, shard does not exist or file corrupt.
     */
    public byte[] retrieveShard(String md5Hash) throws ShardStorageException {
        Path filePath = getPath(basePath.toString(), md5Hash);
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            if (Hashing.md5().hashBytes(bytes).toString().equalsIgnoreCase(md5Hash)) {
                return bytes;
            } else {
                logger.error("Shard corrupted. Removing [{}]", md5Hash);
                Files.delete(filePath);
                throw new ShardStorageException("Shard corrupted.");
            }
        } catch (IOException e) {
            logger.error("Could not retrieve shard [{}]. {}", md5Hash, e.getMessage());
            throw new ShardStorageException("Could not retrieve shard [" + md5Hash + "]. " + e.getMessage());
        }
    }

    /**
     * Removes the shard from the storage shard pool.
     * @param md5Hash the hash of the shard to remove
     * @return length of shard removed
     */
    public long removeShard(String md5Hash) throws ShardStorageException {
        Path filePath = getPath(basePath.toString(), md5Hash);
        try {
            long retLong = filePath.toFile().length();
            Files.delete(filePath);
            for(File folder = filePath.toFile().getParentFile(); !folder.toPath().equals(basePath); folder = folder.getParentFile()) {
                File[] files = folder.listFiles();
                if(files != null && files.length == 0) {
                    Files.delete(folder.toPath());
                } else {
                    break;
                }
            }
            currentStorageSize -= retLong;
            return retLong;
        } catch (IOException e) {
            logger.error("Failed to remove shard {}. {}", md5Hash, e.getMessage());
            throw new ShardStorageException("Cound not remove shard: " + md5Hash + ". " + e.getMessage());
        }
    }

    /**
     * Scans the root folder for shards.
     * @return List of shard md5 hashes available for reading.
     */
    public Set<String> listShards() {
        final Set<Path> memFiles = new HashSet<>();
        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(FILE_EXTENSION)) {
                        memFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Could not find stored shards in [{}].", basePath.toString());
        }
        return memFiles.stream()
                .map(Path::toString)
                .map(x -> x.substring(basePath.toString().length(), x.length() - FILE_EXTENSION.length()))
                .map(x -> x.replaceAll(File.separator, ""))
                .collect(Collectors.toSet());
    }

    /**
     * Constructs the path according to the id given.
     * @param rootPath path to the root folder.
     * @param id The id to use as a folder chain.
     * @return The path the shard should be stored under.
     */
    private Path getPath(String rootPath, String id) {
        StringBuilder currentDir = new StringBuilder().append(rootPath);
        while (id.length() > FOLDER_SPLIT_LEN) {
            currentDir.append(File.separator).append(id.substring(0, FOLDER_SPLIT_LEN));
            id = id.substring(FOLDER_SPLIT_LEN);
        }
        currentDir.append(File.separator).append(id).append(FILE_EXTENSION);
        return Paths.get(currentDir.toString());
    }

    /**
     * Scans the root folder for shards.
     * @return List of shard md5 hashes available for reading.
     */
    public long getStorageSize() {
        final AtomicLong size = new AtomicLong(0);
        if(basePath.toFile().exists()) {
            try {
                Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toString().endsWith(FILE_EXTENSION)) {
                            size.getAndAdd(attrs.size());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.error("Could not find stored shards in [{}].", basePath.toString());
            }
        }
        return size.get();
    }
}
