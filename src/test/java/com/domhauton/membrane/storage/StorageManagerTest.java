package com.domhauton.membrane.storage;

import com.domhauton.membrane.storage.catalogue.JournalEntry;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
import com.google.common.hash.Hashing;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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
  private Path srcFile;
  private Path tgtFile;
  private Random random;

  private final int storageMangerSize = 1024 * 1024 * 1024; //1GB

  private StorageManager storageManager;

  @BeforeEach
  void setUp() throws Exception {
    random = new Random(System.currentTimeMillis());
    testDir = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    srcFile = Paths.get(testDir + File.separator + "src.txt");
    tgtFile = Paths.get(testDir + File.separator + "tgt.txt");
    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);
  }

  @Test
  void testRetrievalOneShards() throws Exception {
    int len = 256;
    byte[] data = new byte[len];
    random.nextBytes(data);

    String hash = Hashing.md5().hashBytes(data).toString();

    storageManager.storeShard(hash, data);

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

    storageManager.storeShard(hash1, data1);
    storageManager.storeShard(hash2, data2);

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

    storageManager.storeShard(hash1, data1);
    storageManager.storeShard(hash2, data2);

    storageManager.addFile(Arrays.asList(md5HashLengthPair1, md5HashLengthPair2), new DateTime(100L), srcFile);

    storageManager.close();
    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);

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

    storageManager.storeShard(hash1, data1);
    storageManager.storeShard(hash2, data2);

    storageManager.addFile(Arrays.asList(md5HashLengthPair1, md5HashLengthPair2), new DateTime(250L), srcFile);

    storageManager.cleanStorage(new DateTime(250L));
    storageManager.close();

    // Check journal file is empty
    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(0, journalSize);
    // Delete to journal to ensure it doesn't interfere anyway.
    Files.delete(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME));

    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);
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
    storageManager.storeShard(hash, data);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L), srcFile);

    random.nextBytes(data);
    hash = Hashing.md5().hashBytes(data).toString();
    md5HashLengthPair = new MD5HashLengthPair(hash, len);
    storageManager.storeShard(hash, data);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L).plusMinutes(1), srcFile);

    random.nextBytes(data);
    hash = Hashing.md5().hashBytes(data).toString();
    md5HashLengthPair = new MD5HashLengthPair(hash, len);
    storageManager.storeShard(hash, data);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L).plusMinutes(2), srcFile);

    storageManager.close();

    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(3, journalSize);

    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);

    storageManager.clampStorageToSize(len, new HashSet<>(Collections.singletonList(srcFile.getParent())));

    storageManager.close();

    List<String> journal = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME));
    Assertions.assertEquals(0, journal.size());

    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);

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

    storageManager.storeShard(hash, data);

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
    storageManager.storeShard(hash, data);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L), srcFile);

    random.nextBytes(data);
    hash = Hashing.md5().hashBytes(data).toString();
    md5HashLengthPair = new MD5HashLengthPair(hash, len);
    storageManager.storeShard(hash, data);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L).plusHours(6), srcFile);

    random.nextBytes(data2);
    hash = Hashing.md5().hashBytes(data2).toString();
    md5HashLengthPair = new MD5HashLengthPair(hash, len);
    storageManager.storeShard(hash, data2);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L).plusHours(12), srcFile);

    storageManager.close();

    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(3, journalSize);

    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);

    storageManager.clampStorageToSize(len * 2, new HashSet<>(Collections.singletonList(srcFile.getParent())));

    storageManager.close();

    List<String> journal = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME))
            .stream()
            .filter(String::isEmpty)
            .collect(Collectors.toList());
    System.err.println(journal);
    Assertions.assertEquals(1, journal.size());

    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);

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
  void testSizeClampingIgnoreFile() throws Exception {
    int len = 16 * 1024 * 1024; //16MB
    byte[] data = new byte[len];

    random.nextBytes(data);
    String hash = Hashing.md5().hashBytes(data).toString();
    MD5HashLengthPair md5HashLengthPair = new MD5HashLengthPair(hash, len);
    storageManager.storeShard(hash, data);
    storageManager.addFile(Collections.singletonList(md5HashLengthPair), new DateTime(100L), srcFile);

    storageManager.close();

    int journalSize = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME)).size();
    Assertions.assertEquals(1, journalSize);

    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);

    storageManager.clampStorageToSize(len - 1, Collections.emptySet());

    storageManager.close();

    List<String> journal = Files.readAllLines(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER + File.separator + StorageManager.JOURNAL_NAME));
    Assertions.assertEquals(0, journal.size());

    storageManager = new StorageManager(Paths.get(testDir), storageMangerSize);

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
    Files.delete(Paths.get(testDir + File.separator + StorageManager.DEFAULT_STORAGE_FOLDER));

    Path catPath = Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER);

    String[] backupPaths = catPath.toFile().list();
    if (backupPaths != null) {
      Arrays.stream(backupPaths)
              .map(s -> catPath + File.separator + s)
              .map(Paths::get)
              .forEach(x -> {
                try {
                  Files.delete(x);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }
    Files.delete(Paths.get(testDir + File.separator + StorageManager.DEFAULT_CATALOGUE_FOLDER));
    Files.delete(Paths.get(testDir));
  }
}