package com.domhauton.membrane.storage;

import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageImpl;
import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by dominic on 01/02/17.
 */
class StorageManagerTest {

  private String testDir;
  private Path shardStoragePath;
  private Path srcFile;
  private Path tgtFile;
  private Random random;

  private final int storageMangerSize = 1024 * 1024 * 1024; //1GB

  private ShardStorage shardStorage;
  private StorageManager storageManager;

  @BeforeEach
  void setUp() throws Exception {
    random = new Random(System.currentTimeMillis());
    testDir = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    shardStoragePath = Paths.get(StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR));
    srcFile = Paths.get(testDir + File.separator + "src.txt");
    tgtFile = Paths.get(testDir + File.separator + "tgt.txt");
    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);
  }

  @Test
  void testRetrievalOneShards() throws Exception {
    int len = 256;
    byte[] data = new byte[len];
    random.nextBytes(data);

    String hash = Hashing.md5().hashBytes(data).toString();

    shardStorage.storeShard(hash, data);
    storageManager.protectShard(hash);

    storageManager.addFile(Collections.singletonList(new MD5HashLengthPair(hash, len)), new DateTime(100L), srcFile);

    storageManager.rebuildFile(srcFile, tgtFile);

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data.length, reconstructed.length);
    Assertions.assertArrayEquals(data, reconstructed);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L));
    storageManager.cleanStorage(new DateTime(250L));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @Test
  void testRetrievalTwoShards() throws Exception {
    int len = 256;
    byte[] data = new byte[len];
    random.nextBytes(data);

    byte[] data1 = Arrays.copyOfRange(data, 0, 128);
    byte[] data2 = Arrays.copyOfRange(data, 128, len);

    String hash1 = Hashing.md5().hashBytes(data1).toString();
    String hash2 = Hashing.md5().hashBytes(data2).toString();

    MD5HashLengthPair md5HashLengthPair1 = new MD5HashLengthPair(hash1, 128);
    MD5HashLengthPair md5HashLengthPair2 = new MD5HashLengthPair(hash2, 128);

    shardStorage.storeShard(hash1, data1);
    storageManager.protectShard(hash1);
    shardStorage.storeShard(hash2, data2);
    storageManager.protectShard(hash2);

    storageManager.addFile(Arrays.asList(md5HashLengthPair1, md5HashLengthPair2), new DateTime(100L), srcFile);

    storageManager.rebuildFile(srcFile, tgtFile);

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data.length, reconstructed.length);
    Assertions.assertArrayEquals(data, reconstructed);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L));
    storageManager.cleanStorage(new DateTime(250L));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @Test
  void testJournalStorageTest() throws Exception {
    int len = 256;
    byte[] data = new byte[len];
    random.nextBytes(data);

    byte[] data1 = Arrays.copyOfRange(data, 0, len / 2);
    byte[] data2 = Arrays.copyOfRange(data, len / 2, len);

    String hash1 = Hashing.md5().hashBytes(data1).toString();
    String hash2 = Hashing.md5().hashBytes(data2).toString();

    MD5HashLengthPair md5HashLengthPair1 = new MD5HashLengthPair(hash1, len / 2);
    MD5HashLengthPair md5HashLengthPair2 = new MD5HashLengthPair(hash2, len - (len / 2));

    shardStorage.storeShard(hash1, data1);
    storageManager.protectShard(hash1);
    shardStorage.storeShard(hash2, data2);
    storageManager.protectShard(hash2);

    storageManager.addFile(Arrays.asList(md5HashLengthPair1, md5HashLengthPair2), new DateTime(100L), srcFile);

    storageManager.close();

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    storageManager.rebuildFile(srcFile, tgtFile);

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data.length, reconstructed.length);
    Assertions.assertArrayEquals(data, reconstructed);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L));
    storageManager.cleanStorage(new DateTime(250L));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @Test
  void testBaseFileMapStorageTest() throws Exception {
    int len = 256;
    byte[] data = new byte[len];
    random.nextBytes(data);

    byte[] data1 = Arrays.copyOfRange(data, 0, len / 2);
    byte[] data2 = Arrays.copyOfRange(data, len / 2, len);

    String hash1 = Hashing.md5().hashBytes(data1).toString();
    String hash2 = Hashing.md5().hashBytes(data2).toString();

    MD5HashLengthPair md5HashLengthPair1 = new MD5HashLengthPair(hash1, len / 2);
    MD5HashLengthPair md5HashLengthPair2 = new MD5HashLengthPair(hash2, len - (len / 2));

    shardStorage.storeShard(hash1, data1);
    storageManager.protectShard(hash1);
    shardStorage.storeShard(hash2, data2);
    storageManager.protectShard(hash2);

    storageManager.addFile(Arrays.asList(md5HashLengthPair1, md5HashLengthPair2), new DateTime(250L), srcFile);

    storageManager.cleanStorage(new DateTime(250L));
    storageManager.close();

    // Check journal file is empty
    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(0, journalSize);
    // Delete to journal to ensure it doesn't interfere anyway.
    Files.delete(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME));

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);
    storageManager.rebuildFile(srcFile, tgtFile);

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data.length, reconstructed.length);
    Assertions.assertArrayEquals(data, reconstructed);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L));
    storageManager.cleanStorage(new DateTime(250L));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }


  @Test
  void addAfterCleanTest() throws Exception {
    int len = 256;
    byte[] data = new byte[len];
    random.nextBytes(data);

    byte[] data1 = Arrays.copyOfRange(data, 0, len / 2);
    byte[] data2 = Arrays.copyOfRange(data, len / 2, len);

    String hash1 = Hashing.md5().hashBytes(data1).toString();
    String hash2 = Hashing.md5().hashBytes(data2).toString();

    MD5HashLengthPair md5HashLengthPair1 = new MD5HashLengthPair(hash1, len / 2);
    MD5HashLengthPair md5HashLengthPair2 = new MD5HashLengthPair(hash2, len - (len / 2));

    shardStorage.storeShard(hash1, data1);
    storageManager.protectShard(hash1);
    shardStorage.storeShard(hash2, data2);
    storageManager.protectShard(hash2);

    storageManager.addFile(Arrays.asList(md5HashLengthPair1, md5HashLengthPair2), new DateTime(250L), srcFile);

    storageManager.cleanStorage(new DateTime(250L));
    storageManager.close();


    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    storageManager.addFile(Collections.singletonList(md5HashLengthPair1), new DateTime(150L), srcFile);

    Set<Long> modifyTimeSet = storageManager.getFileHistory(srcFile).stream().map(JournalEntry::getDateTime).map(DateTime::getMillis).collect(Collectors.toSet());
    Assertions.assertEquals(new HashSet<>(Arrays.asList(150L, 250L)), modifyTimeSet);

    storageManager.rebuildFile(srcFile, tgtFile);

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data.length, reconstructed.length);
    Assertions.assertArrayEquals(data, reconstructed);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L));
    storageManager.cleanStorage(new DateTime(250L));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @Test
  void testSizeClamping() throws Exception {
    int len = 16 * 1024 * 1024; //16MB
    byte[] data = new byte[len];

    random.nextBytes(data);
    String hash = Hashing.md5().hashBytes(data).toString();
    MD5HashLengthPair md5HashLengthPair = new MD5HashLengthPair(hash, len);
    shardStorage.storeShard(hash, data);
    storageManager.protectShard(hash);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L), srcFile);

    random.nextBytes(data);
    hash = Hashing.md5().hashBytes(data).toString();
    md5HashLengthPair = new MD5HashLengthPair(hash, len);
    shardStorage.storeShard(hash, data);
    storageManager.protectShard(hash);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L).plusMinutes(1), srcFile);

    random.nextBytes(data);
    hash = Hashing.md5().hashBytes(data).toString();
    md5HashLengthPair = new MD5HashLengthPair(hash, len);
    shardStorage.storeShard(hash, data);
    storageManager.protectShard(hash);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L).plusMinutes(2), srcFile);

    storageManager.close();

    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(3, journalSize);

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    storageManager.clampStorageToSize(len, new HashSet<>(Collections.singletonList(srcFile.getParent())));

    storageManager.close();

    List<String> journal = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME));
    Assertions.assertEquals(0, journal.size());

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    List<JournalEntry> journalEntries = storageManager.getFileHistory(srcFile);
    Assertions.assertEquals(1, journalEntries.size());
    storageManager.rebuildFile(srcFile, tgtFile, journalEntries.get(0).getDateTime());

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data.length, reconstructed.length);
    Assertions.assertArrayEquals(data, reconstructed);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L).plusDays(1));
    storageManager.cleanStorage(new DateTime(250L).plusDays(1));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @Test
  void ensureNoOverwriteOnRebuildTest() throws Exception {
    int len = 256;
    byte[] data = new byte[len];
    random.nextBytes(data);

    String hash = Hashing.md5().hashBytes(data).toString();

    shardStorage.storeShard(hash, data);
    storageManager.protectShard(hash);

    storageManager.addFile(Collections.singletonList(new MD5HashLengthPair(hash, len)), new DateTime(100L), srcFile);

    byte[] data2 = new byte[len];
    random.nextBytes(data2);

    Files.write(tgtFile, data2);

    assertThrows(StorageManagerException.class, () -> storageManager.rebuildFile(srcFile, tgtFile));

    Files.delete(tgtFile);

    storageManager.rebuildFile(srcFile, tgtFile);

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data.length, reconstructed.length);
    Assertions.assertArrayEquals(data, reconstructed);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L));
    storageManager.cleanStorage(new DateTime(250L));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @Test
  void testSizeClampingPartial() throws Exception {
    int len = 16 * 1024 * 1024; //16MB
    byte[] data = new byte[len];
    byte[] data2 = new byte[len];

    random.nextBytes(data);
    String hash = Hashing.md5().hashBytes(data).toString();
    MD5HashLengthPair md5HashLengthPair = new MD5HashLengthPair(hash, len);
    shardStorage.storeShard(hash, data);
    storageManager.protectShard(hash);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L), srcFile);

    random.nextBytes(data);
    hash = Hashing.md5().hashBytes(data).toString();
    md5HashLengthPair = new MD5HashLengthPair(hash, len);
    shardStorage.storeShard(hash, data);
    storageManager.protectShard(hash);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L).plusHours(6), srcFile);

    random.nextBytes(data2);
    hash = Hashing.md5().hashBytes(data2).toString();
    md5HashLengthPair = new MD5HashLengthPair(hash, len);
    shardStorage.storeShard(hash, data2);
    storageManager.protectShard(hash);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L).plusHours(12), srcFile);

    storageManager.close();

    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(3, journalSize);

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    storageManager.clampStorageToSize(len * 2, new HashSet<>(Collections.singletonList(srcFile.getParent())));

    storageManager.close();

    List<String> journal = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME))
            .stream()
            .filter(String::isEmpty)
            .collect(Collectors.toList());
    System.err.println(journal);
    Assertions.assertEquals(1, journal.size());

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    List<JournalEntry> journalEntries = storageManager.getFileHistory(srcFile);
    Assertions.assertEquals(2, journalEntries.size());
    storageManager.rebuildFile(srcFile, tgtFile, journalEntries.get(0).getDateTime());

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data.length, reconstructed.length);
    Assertions.assertArrayEquals(data, reconstructed);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L).plusDays(1));
    storageManager.cleanStorage(new DateTime(250L).plusDays(1));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @Test
  void testSizeClampingPartialOutOfOrder() throws Exception {
    int len = 16 * 1024 * 1024; //16MB
    byte[] data1 = new byte[len];
    byte[] data2 = new byte[len];
    byte[] data3 = new byte[len];

    random.nextBytes(data2);
    String hash2 = Hashing.md5().hashBytes(data2).toString();
    MD5HashLengthPair md5HashLengthPair2 = new MD5HashLengthPair(hash2, len);
    shardStorage.storeShard(hash2, data2);
    storageManager.protectShard(hash2);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair2), new DateTime(100L).plusHours(6), srcFile);

    random.nextBytes(data1);
    String hash1 = Hashing.md5().hashBytes(data1).toString();
    MD5HashLengthPair md5HashLengthPair1 = new MD5HashLengthPair(hash1, len);
    shardStorage.storeShard(hash1, data1);
    storageManager.protectShard(hash1);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair1), new DateTime(100L), srcFile);

    for (JournalEntry journalEntry : storageManager.getFileHistory(srcFile)) {
      String s = journalEntry.toString();
      storageManager.insertJournalEntry(s);
    }

    Assertions.assertEquals(2, storageManager.getFileHistory(srcFile).size());
    storageManager.rebuildFile(srcFile, tgtFile);

    byte[] reconstructed = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data2.length, reconstructed.length);
    Assertions.assertArrayEquals(data2, reconstructed);

    Files.delete(tgtFile);

    random.nextBytes(data3);
    String hash3 = Hashing.md5().hashBytes(data3).toString();
    MD5HashLengthPair md5HashLengthPair3 = new MD5HashLengthPair(hash3, len);
    shardStorage.storeShard(hash3, data3);
    storageManager.protectShard(hash3);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair3), new DateTime(100L).plusHours(12), srcFile);

    storageManager.close();

    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(3, journalSize);

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    storageManager.clampStorageToSize(len * 2, new HashSet<>(Collections.singletonList(srcFile.getParent())));

    storageManager.close();

    List<String> journal = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME))
        .stream()
        .filter(String::isEmpty)
        .collect(Collectors.toList());
    System.err.println(journal);
    Assertions.assertEquals(1, journal.size());

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    List<JournalEntry> journalEntries = storageManager.getFileHistory(srcFile);
    Assertions.assertEquals(2, journalEntries.size());

    storageManager.rebuildFile(srcFile, tgtFile, journalEntries.get(0).getDateTime());

    byte[] reconstructed2 = Files.readAllBytes(tgtFile);

    Assertions.assertEquals(data2.length, reconstructed2.length);
    Assertions.assertArrayEquals(data2, reconstructed2);

    Files.delete(tgtFile);

    storageManager.removeFile(srcFile, new DateTime(200L).plusDays(1));
    storageManager.cleanStorage(new DateTime(250L).plusDays(1));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @Test
  void testSizeClampingIgnoreFile() throws Exception {
    int len = 16 * 1024 * 1024; //16MB
    byte[] data = new byte[len];

    random.nextBytes(data);
    String hash = Hashing.md5().hashBytes(data).toString();
    MD5HashLengthPair md5HashLengthPair = new MD5HashLengthPair(hash, len);
    shardStorage.storeShard(hash, data);
    storageManager.protectShard(hash);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L), srcFile);

    storageManager.close();

    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(1, journalSize);

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    storageManager.clampStorageToSize(len - 1, Collections.emptySet());

    storageManager.close();

    List<String> journal = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME));
    Assertions.assertEquals(0, journal.size());

    shardStorage = new ShardStorageImpl(shardStoragePath, storageMangerSize);
    storageManager = new StorageManager(Paths.get(testDir), shardStorage);

    List<JournalEntry> journalEntries = storageManager.getFileHistory(srcFile);
    Assertions.assertEquals(0, journalEntries.size());

    storageManager.removeFile(srcFile, new DateTime(200L).plusDays(1));
    storageManager.cleanStorage(new DateTime(250L).plusDays(1));
    storageManager.clearProtectedShards();
    storageManager.collectGarbage();
  }

  @AfterEach
  void tearDown() throws Exception {
    storageManager.close();
    StorageManagerTestUtils.deleteDirectoryRecursively(Paths.get(testDir));
    StorageManagerTestUtils.deleteDirectoryRecursively(shardStoragePath);
  }
}