package com.domhauton.membrane.storage;

import com.google.common.hash.Hashing;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * Created by dominic on 01/02/17.
 */
class StorageManagerTest {

    private String testDir;
    private Path srcFile;
    private Path tgtFile;
    private Random random;

    private StorageManager storageManager;

    @BeforeEach
    void setUp() throws Exception {
        random = new Random(System.currentTimeMillis());
        testDir = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
        srcFile = Paths.get(testDir + File.separator + "src.txt");
        tgtFile = Paths.get(testDir + File.separator + "tgt.txt");
        storageManager = new StorageManager(Paths.get(testDir));
    }

    @Test
    void testRetrievalOneShards() throws Exception {
        byte[] data = new byte[256];
        random.nextBytes(data);

        String hash = Hashing.md5().hashBytes(data).toString();

        storageManager.storeShard(hash, data);

        storageManager.addFile(Collections.singletonList(hash), new DateTime(100L), srcFile);

        storageManager.rebuildFile(srcFile, tgtFile);

        byte[] reconstructed = Files.readAllBytes(tgtFile);

        Assertions.assertEquals(data.length, reconstructed.length);
        Assertions.assertArrayEquals(data, reconstructed);

        Files.delete(tgtFile);

        storageManager.removeFile(srcFile, new DateTime(200L));
        storageManager.cleanStorage(new DateTime(250L));
        storageManager.clearProtectedShards();
        storageManager.collectGarbage();

        Files.delete(Paths.get(testDir + File.separator + StorageManager.DEFAULT_STORAGE_FOLDER));
    }

    @Test
    void testRetrievalTwoShards() throws Exception {
        byte[] data = new byte[256];
        random.nextBytes(data);

        byte[] data1 = Arrays.copyOfRange(data, 0, 128);
        byte[] data2 = Arrays.copyOfRange(data, 128, 256);

        String hash1 = Hashing.md5().hashBytes(data1).toString();
        String hash2 = Hashing.md5().hashBytes(data2).toString();

        storageManager.storeShard(hash1, data1);
        storageManager.storeShard(hash2, data2);

        storageManager.addFile(Arrays.asList(hash1, hash2), new DateTime(100L), srcFile);

        storageManager.rebuildFile(srcFile, tgtFile);

        byte[] reconstructed = Files.readAllBytes(tgtFile);

        Assertions.assertEquals(data.length, reconstructed.length);
        Assertions.assertArrayEquals(data, reconstructed);

        Files.delete(tgtFile);

        storageManager.removeFile(srcFile, new DateTime(200L));
        storageManager.cleanStorage(new DateTime(250L));
        storageManager.clearProtectedShards();
        storageManager.collectGarbage();

        Files.delete(Paths.get(testDir + File.separator + StorageManager.DEFAULT_STORAGE_FOLDER));
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.delete(Paths.get(testDir));
    }
}