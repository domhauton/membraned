package com.domhauton.membrane.storage.shard;

import com.domhauton.membrane.storage.StorageManagerTestUtils;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by dominic on 01/02/17.
 */
class ShardStorageImplTest {
    private ShardStorageImpl shardStorage;
    private String testDir;
    private Random random;

    @BeforeEach
    void setUp() throws Exception {
        testDir = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
        shardStorage = new ShardStorageImpl(Paths.get(testDir), 1024*1024*1024);
        random = new Random(System.currentTimeMillis());
    }

    @Test
    void testCreateAndRetrieve() throws Exception {
        String addedFile = StorageManagerTestUtils.addRandFile(random, shardStorage);
        byte[] loadedData = shardStorage.retrieveShard(addedFile);
        String calculatedHash = Hashing.md5().hashBytes(loadedData).toString();

        Assertions.assertEquals(addedFile, calculatedHash);

        shardStorage.removeShard(addedFile);
        Files.delete(Paths.get(testDir));
    }

    @Test
    void testCreateAndList() throws Exception {
        String addedFile1 = StorageManagerTestUtils.addRandFile(random, shardStorage);
        String addedFile2 = StorageManagerTestUtils.addRandFile(random, shardStorage);
        Set<String> list = shardStorage.listShards();
        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.contains(addedFile1));
        Assertions.assertTrue(list.contains(addedFile2));

        shardStorage.removeShard(addedFile1);
        shardStorage.removeShard(addedFile2);
        Files.delete(Paths.get(testDir));
    }

    @Test
    void testCreateAndRemoval() throws Exception {
        String addedFile = StorageManagerTestUtils.addRandFile(random, shardStorage);
        Assertions.assertEquals(1, shardStorage.listShards().size());

        shardStorage.removeShard(addedFile);
        Assertions.assertEquals(0, shardStorage.listShards().size());
        Files.delete(Paths.get(testDir));
    }
}