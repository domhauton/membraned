package com.domhauton.membrane.storage.shard;

import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/01/17.
 */
public class ShardStorageImpl implements ShardStorage {
    private static final String FILE_EXTENSION = ".mem";
    private static final int FOLDER_SPLIT_LEN = 5;
    private static final String DEFAULT_BASE_PATH = System.getProperty("user.home") + File.separator + ".membrane";

    private Logger logger;
    private Path basePath;

    public ShardStorageImpl() {
        this(Paths.get(DEFAULT_BASE_PATH));
    }

    /**
     * @param basePath Directory to store shards.
     */
    public ShardStorageImpl(Path basePath) {
        this.basePath = basePath;
        logger = LogManager.getLogger();
    }

    /**
     * Store the data given under the given hash.
     * @param md5Hash The md5Hash of the data given
     * @param data The data to store.
     * @throws ShardStorageException
     */
    public void storeShard(String md5Hash, byte[] data) throws ShardStorageException {
        Path filePath = getPath(basePath.toString(), md5Hash);
        try {
            boolean success = filePath.toFile().getParentFile().mkdirs();
            logger.debug("Created directories for [{}]: {}", filePath, success);
            Files.write(filePath, data);
        } catch (IOException e) {
            logger.error("Could not store shard [{}]. {}", md5Hash, e.getMessage());
            throw new ShardStorageException("Could not store shard [" + md5Hash + "]. " + e.getMessage());
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
     * @throws ShardStorageException
     */
    public void removeShard(String md5Hash) throws ShardStorageException {
        Path filePath = getPath(basePath.toString(), md5Hash);
        try {
            Files.delete(filePath);
            for(File folder = filePath.toFile().getParentFile(); !folder.toPath().equals(basePath); folder = folder.getParentFile()) {
                File[] files = folder.listFiles();
                if(files != null && files.length == 0) {
                    Files.delete(folder.toPath());
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
}
