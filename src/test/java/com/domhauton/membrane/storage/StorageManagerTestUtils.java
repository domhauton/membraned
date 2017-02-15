package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.shard.ShardStorage;
import com.google.common.hash.Hashing;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by dominic on 01/02/17.
 */
public abstract class StorageManagerTestUtils {

    public static final String BASE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "membrane";

    public static String addRandFile(Random random, ShardStorage shardStorage) throws Exception {
        byte[] data = new byte[128];
        random.nextBytes(data);
        String hash = Hashing.md5().hashBytes(data).toString();
        shardStorage.storeShard(hash, data);
        return hash;
    }

    public static String createRandomFolder(String baseDir) throws Exception {
        String tmpDir = baseDir;
        Path tmpPath = Paths.get(baseDir);
        Files.createDirectories(tmpPath);
        while(Files.exists(tmpPath, LinkOption.NOFOLLOW_LINKS)) {
            tmpDir = baseDir + File.separator + new BigInteger(16, new SecureRandom()).toString(32);
            tmpPath = Paths.get(tmpDir);
        }
        Files.createDirectories(tmpPath);
        return tmpDir;
    }
}
