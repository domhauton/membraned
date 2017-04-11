package com.domhauton.membrane.storage;

import com.domhauton.membrane.shard.ShardStorage;
import com.google.common.hash.Hashing;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by dominic on 01/02/17.
 */
public abstract class StorageManagerTestUtils {

  public static final String BASE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "membrane";
  public static final long RAND_SHARD_SIZE = 128;
  public static final SecureRandom secureRandom = new SecureRandom();

  public static String addRandFile(Random random, ShardStorage shardStorage) throws Exception {
    byte[] data = new byte[(int) RAND_SHARD_SIZE];
    random.nextBytes(data);
    String hash = Hashing.md5().hashBytes(data).toString();
    shardStorage.storeShard(hash, data);
    return hash;
  }

  public static String createRandomFolder(String baseDir) throws Exception {
    String tmpDir = baseDir;
    Path tmpPath = Paths.get(baseDir);
    Files.createDirectories(tmpPath);
    while (Files.exists(tmpPath, LinkOption.NOFOLLOW_LINKS)) {
      tmpDir = baseDir + File.separator + new BigInteger(16, secureRandom).toString(32);
      tmpPath = Paths.get(tmpDir);
    }
    Files.createDirectories(tmpPath);
    return tmpDir;
  }

  public static void deleteDirectoryRecursively(Path folder) throws IOException {
    if (folder.toString().startsWith(BASE_DIR)) {
      Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
          // Try to delete the file anyway
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } else {
      throw new IOException("Tried to delete non " + BASE_DIR + " folder.");
    }
  }
}
