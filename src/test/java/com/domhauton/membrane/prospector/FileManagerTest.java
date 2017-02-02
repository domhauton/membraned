package com.domhauton.membrane.prospector;

import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.storage.StorageManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Created by dominic on 31/01/17.
 */
class FileManagerTest {

    private Logger logger = LogManager.getLogger();

    private static final String BASE_DIR = "/tmp";
    private FileManager fileManager;
    private String dir;

    @BeforeEach
    void setUp() throws Exception {
        fileManager = new FileManager(64);
        dir = ProspectorTestUtils.createRandomFolder(BASE_DIR);
        logger.info("Setting up in [{}]", dir);
    }

    @Test
    void testFindFoldersOnLoad() throws Exception {
        ProspectorTestUtils.createTestFiles(dir);
        StorageManager storageManager = Mockito.mock(StorageManager.class);

        WatchFolder watchFolder = new WatchFolder(dir, false);
        fileManager.addStorageManager(storageManager);
        fileManager.addWatchFolder(watchFolder);

        Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
                .addFile(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
                .storeShard(Mockito.any(), Mockito.any(byte[].class));

        ProspectorTestUtils.removeTestFiles(dir);
        Files.delete(Paths.get(dir));
    }

    @Test
    void testFindFoldersOnWait() throws Exception {
        StorageManager storageManager = Mockito.mock(StorageManager.class);

        WatchFolder watchFolder = new WatchFolder(dir, true);
        fileManager.addWatchFolder(watchFolder);
        fileManager.addStorageManager(storageManager);
        String embeddedDir = ProspectorTestUtils.createRandomFolder(dir);
        ProspectorTestUtils.createTestFiles(embeddedDir);
        fileManager.checkFolderChanges();

        Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
                .addFile(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
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
        fileManager.addStorageManager(storageManager);

        fileManager.addExistingFile(Paths.get(dir + File.separator + '0'), DateTime.now(), Collections.emptyList());

        String embeddedDir = ProspectorTestUtils.createRandomFolder(dir);

        fileManager.addExistingFile(Paths.get(embeddedDir + File.separator + '0'), DateTime.now(), Collections.emptyList());

        ProspectorTestUtils.createTestFiles(embeddedDir);
        fileManager.checkFolderChanges();

        Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.CREATED_FILES_COUNT))
                .addFile(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(storageManager, Mockito.times(ProspectorTestUtils.EXPECTED_SHARD_COUNT))
                .storeShard(Mockito.any(), Mockito.any(byte[].class));
        Mockito.verify(storageManager, Mockito.times(1))
                .removeFile(Mockito.any(), Mockito.any());

        ProspectorTestUtils.removeTestFiles(embeddedDir);
        Files.delete(Paths.get(embeddedDir));
        Files.delete(Paths.get(dir));
    }
}