package com.domhauton.membrane.prospector;

import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.shard.ShardStorage;
import com.domhauton.membrane.shard.ShardStorageException;
import com.domhauton.membrane.storage.FileEventLogger;
import com.domhauton.membrane.storage.FileEventLoggerImpl;
import com.domhauton.membrane.storage.StorageManager;
import com.domhauton.membrane.storage.StorageManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Created by dominic on 31/01/17.
 */
class FileManagerTest {

  private final Logger logger = LogManager.getLogger();

  private static final String BASE_DIR = "/tmp";
  private FileManager fileManager;
  private FileEventLogger fileEventLoggerTemp;
  private ShardStorage shardStorageMock;
  private String dir;

  @BeforeEach
  void setUp() throws Exception {
    fileEventLoggerTemp = new FileEventLoggerImpl();
    shardStorageMock = Mockito.mock(ShardStorage.class);
    fileManager = new FileManager(fileEventLoggerTemp, shardStorageMock, 64);
    dir = ProspectorTestUtils.createRandomFolder(BASE_DIR);
    logger.info("Setting up in [{}]", dir);
  }

  @Test
  void testFindFoldersOnLoad() throws Exception {
    ProspectorTestUtils.createTestFiles(dir);
    StorageManager storageManager = Mockito.mock(StorageManager.class);

    WatchFolder watchFolder = new WatchFolder(dir, false);
    fileManager.setFileEventLogger(storageManager);
    fileManager.addWatchFolder(watchFolder);
    fileManager.fullFileScanSweep();

    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));

    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, fileManager.getCurrentlyWatchedFiles().size());

    ProspectorTestUtils.removeTestFiles(dir);
    Files.delete(Paths.get(dir));
  }

  @Test
  void retryOnStoreShardFailTest() throws Exception {
    ProspectorTestUtils.createTestFiles(dir);
    StorageManager storageManager = Mockito.mock(StorageManager.class);

    Mockito.doThrow(new ShardStorageException("Mock Exception"))
        .when(shardStorageMock)
            .storeShard(Mockito.anyString(), Mockito.any(byte[].class));

    WatchFolder watchFolder = new WatchFolder(dir, false);
    fileManager.setFileEventLogger(storageManager);
    fileManager.addWatchFolder(watchFolder);
    fileManager.fullFileScanSweep();

    Mockito.verify(storageManager, Mockito.never())
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));

    Assertions.assertEquals(0, fileManager.getCurrentlyWatchedFiles().size());

    fileManager.checkFileChanges();

    // Should try to re-add files

    Assertions.assertEquals(0, fileManager.getCurrentlyWatchedFiles().size());

    Mockito.verify(storageManager, Mockito.never())
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT * 2))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT * 2))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));

    ProspectorTestUtils.removeTestFiles(dir);

    fileManager.checkFileChanges();

    // Should have tried to re-scan deleted files and failed.

    Assertions.assertEquals(0, fileManager.getCurrentlyWatchedFiles().size());

    Mockito.verify(storageManager, Mockito.never())
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT * 2))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT * 2))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));

    Files.delete(Paths.get(dir));
  }

  @Test
  void retryOnStoreHeaderFailTest() throws Exception {
    ProspectorTestUtils.createTestFiles(dir);
    StorageManager storageManager = Mockito.mock(StorageManager.class);

    Mockito.doThrow(new StorageManagerException("Mock Exception"))
            .when(storageManager)
            .addFile(Mockito.anyList(), Mockito.any(DateTime.class), Mockito.any(Path.class));

    WatchFolder watchFolder = new WatchFolder(dir, false);
    fileManager.setFileEventLogger(storageManager);
    fileManager.addWatchFolder(watchFolder);
    fileManager.fullFileScanSweep();

    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));

    Assertions.assertEquals(0, fileManager.getCurrentlyWatchedFiles().size());

    fileManager.checkFileChanges();

    // Should try to re-add files

    Assertions.assertEquals(0, fileManager.getCurrentlyWatchedFiles().size());

    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT * 2))
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT * 2))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT * 2))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));

    ProspectorTestUtils.removeTestFiles(dir);

    fileManager.checkFileChanges();

    // Should have tried to re-scan deleted files and failed.

    Assertions.assertEquals(0, fileManager.getCurrentlyWatchedFiles().size());

    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT * 2))
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT * 2))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT * 2))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));

    Files.delete(Paths.get(dir));
  }

  @Test
  void testFindFoldersOnWait() throws Exception {
    StorageManager storageManager = Mockito.mock(StorageManager.class);

    WatchFolder watchFolder = new WatchFolder(dir, true);
    fileManager.addWatchFolder(watchFolder);
    fileManager.setFileEventLogger(storageManager);
    String embeddedDir = ProspectorTestUtils.createRandomFolder(dir);
    ProspectorTestUtils.createTestFiles(embeddedDir);
    fileManager.fullFileScanSweep();

    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));

    ProspectorTestUtils.removeTestFiles(embeddedDir);
    Files.delete(Paths.get(embeddedDir));
    Files.delete(Paths.get(dir));
  }

  @Test
  void testRecogniseMissingFile() throws Exception {
    StorageManager storageManager = Mockito.mock(StorageManager.class);

    WatchFolder watchFolder = new WatchFolder(dir, true);
    fileManager.addWatchFolder(watchFolder);
    fileManager.setFileEventLogger(storageManager);

    fileManager.addExistingFile(Paths.get(dir + File.separator + '0'), DateTime.now(), Collections.emptyList());

    String embeddedDir = ProspectorTestUtils.createRandomFolder(dir);

    fileManager.addExistingFile(Paths.get(embeddedDir + File.separator + '0'), DateTime.now(), Collections.emptyList());

    ProspectorTestUtils.createTestFiles(embeddedDir);
    fileManager.fullFileScanSweep();

    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
            .addFile(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
        .protectShard(Mockito.any());
    Mockito.verify(shardStorageMock, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
        .storeShard(Mockito.any(), Mockito.any(byte[].class));
    Mockito.verify(storageManager, Mockito.times(1))
            .removeFile(Mockito.any(), Mockito.any());

    ProspectorTestUtils.removeTestFiles(embeddedDir);
    Files.delete(Paths.get(embeddedDir));
    Files.delete(Paths.get(dir));
  }
}