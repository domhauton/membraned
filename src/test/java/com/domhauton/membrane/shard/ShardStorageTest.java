package com.domhauton.membrane.shard;

import com.domhauton.membrane.storage.StorageManagerTestUtils;
import com.google.common.hash.Hashing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.Set;

/**
 * Created by dominic on 01/02/17.
 */
class ShardStorageTest {
  private ShardStorageImpl shardStorage;
  private String testDir;
  private Random random;

  @BeforeEach
  void setUp() throws Exception {
    testDir = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    random = new Random(System.currentTimeMillis());
  }

  @Test
  void testCreateAndRetrieve() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 1024 * 1024 * 1024);
    String addedFile = StorageManagerTestUtils.addRandFile(random, shardStorage);
    byte[] loadedData = shardStorage.retrieveShard(addedFile);
    String calculatedHash = Hashing.md5().hashBytes(loadedData).toString();

    Assertions.assertEquals(addedFile, calculatedHash);

    shardStorage.removeShard(addedFile);
    Files.delete(Paths.get(testDir));
  }

  @Test
  void testCreateAndCheck() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 1024 * 1024 * 1024);
    String addedFile = StorageManagerTestUtils.addRandFile(random, shardStorage);
    Assertions.assertTrue(shardStorage.hasShard(addedFile));
    shardStorage.removeShard(addedFile);
    Assertions.assertFalse(shardStorage.hasShard(addedFile));
    Files.delete(Paths.get(testDir));
  }

  @Test
  void testCreateAndGetSize() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 1024 * 1024 * 1024);
    String addedFile = StorageManagerTestUtils.addRandFile(random, shardStorage);
    long shardSize = shardStorage.getShardSize(addedFile);
    Assertions.assertEquals(StorageManagerTestUtils.RAND_SHARD_SIZE, shardSize);

    shardStorage.removeShard(addedFile);
    Files.delete(Paths.get(testDir));
  }

  @Test
  void testGetSizeFail() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 1024 * 1024 * 1024);
    Assertions.assertThrows(ShardStorageException.class, () -> shardStorage.getShardSize("artsratasta"));
    Files.delete(Paths.get(testDir));
  }

  @Test
  void testCreateAndList() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 1024 * 1024 * 1024);
    String addedFile1 = StorageManagerTestUtils.addRandFile(random, shardStorage);
    String addedFile2 = StorageManagerTestUtils.addRandFile(random, shardStorage);
    Set<String> list = shardStorage.listShardIds();
    Assertions.assertEquals(2, list.size());
    Assertions.assertTrue(list.contains(addedFile1));
    Assertions.assertTrue(list.contains(addedFile2));

    shardStorage.removeShard(addedFile1);
    shardStorage.removeShard(addedFile2);
    Files.delete(Paths.get(testDir));
  }

  @Test
  void testCreateAndRemoval() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 1024 * 1024 * 1024);
    String addedFile = StorageManagerTestUtils.addRandFile(random, shardStorage);
    Assertions.assertEquals(1, shardStorage.listShardIds().size());

    shardStorage.removeShard(addedFile);
    Assertions.assertEquals(0, shardStorage.listShardIds().size());
    Files.delete(Paths.get(testDir));
  }

  @Test
  void insufficientStorageTest() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 257);
    String addedFile1 = StorageManagerTestUtils.addRandFile(random, shardStorage);
    String addedFile2 = StorageManagerTestUtils.addRandFile(random, shardStorage);
    Assertions.assertThrows(ShardStorageException.class, () -> StorageManagerTestUtils.addRandFile(random, shardStorage));
    Assertions.assertEquals(2, shardStorage.listShardIds().size());
    shardStorage.removeShard(addedFile1);
    Assertions.assertEquals(1, shardStorage.listShardIds().size());
    shardStorage.removeShard(addedFile2);
    Assertions.assertEquals(0, shardStorage.listShardIds().size());
    Files.delete(Paths.get(testDir));
  }

  @Test
  void insufficientPermissionsTest() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 257);
    Assertions.assertTrue(Paths.get(testDir).toFile().setWritable(false));
    Assertions.assertThrows(ShardStorageException.class, () -> StorageManagerTestUtils.addRandFile(random, shardStorage));
    Assertions.assertEquals(0, shardStorage.listShardIds().size());
    Assertions.assertTrue(Paths.get(testDir).toFile().setWritable(true));
    String addedFile1 = StorageManagerTestUtils.addRandFile(random, shardStorage);
    Assertions.assertEquals(1, shardStorage.listShardIds().size());
    shardStorage.removeShard(addedFile1);
    Assertions.assertEquals(0, shardStorage.listShardIds().size());
    Files.delete(Paths.get(testDir));
  }

  @Test
  void corruptedShardTest() throws Exception {
    shardStorage = new ShardStorageImpl(Paths.get(testDir), 257);
    String addedFile1 = StorageManagerTestUtils.addRandFile(random, shardStorage);
    Path shardLocation = shardStorage.getPath(testDir, addedFile1);
    Files.write(shardLocation, " ".getBytes(), StandardOpenOption.APPEND);
    Assertions.assertEquals(1, shardStorage.listShardIds().size());
    Assertions.assertThrows(ShardStorageException.class, () -> shardStorage.retrieveShard(addedFile1));
    Assertions.assertEquals(0, shardStorage.listShardIds().size());
    Assertions.assertThrows(ShardStorageException.class, () -> shardStorage.removeShard(addedFile1));
    Assertions.assertEquals(0, shardStorage.listShardIds().size());
    Files.delete(Paths.get(testDir));
  }
}