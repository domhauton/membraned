package com.domhauton.membrane;

import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.items.WatchFolder;
import com.domhauton.membrane.storage.StorageManagerException;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by dominic on 02/02/17.
 */
class BackupManagerTest {

    private Config config;
    private Path basePath;
    private Path configPath;

    private Path fileBaseFolder;
    private Path fileFolder1;
    private Path fileFolder2;

    private Path testFile1;
    private Path testFile2;
    private Path testFile3;

    private Path recoveryDest;

    private Random random;

    private BackupManager backupManager;

    @BeforeEach
    void setUp() throws Exception {
        random = new Random(System.currentTimeMillis());

        basePath = getBaseFolder();
        configPath = Paths.get(basePath + File.separator + "membrane.yaml");
        fileBaseFolder = Paths.get(basePath + File.separator + "dummyFiles");
        fileFolder1 = Paths.get(fileBaseFolder + File.separator + "fold1");
        fileFolder2 = Paths.get(fileBaseFolder + File.separator + "fold2" + File.separator + "foldInner");

        testFile1 = Paths.get(fileFolder1 + File.separator + "file1.txt");
        testFile2 = Paths.get(fileFolder2.getParent() + File.separator + "file2.txt");
        testFile3 = Paths.get(fileFolder2 + File.separator + "file3.txt");

        recoveryDest = Paths.get(basePath + File.separator + "recoveredFile");

        createTestFolders();

        WatchFolder watchFolder1 = new WatchFolder(fileFolder1.toString(), false);
        WatchFolder watchFolder2 = new WatchFolder(fileFolder2.getParent().toString(), true);

        config = new Config(
                basePath.toString() + File.separator + "shardStorage",
                1,
                10,
                1,
                4,
                16,
                30,
                Arrays.asList(watchFolder1, watchFolder2));

        backupManager = new BackupManager(config, configPath);
        backupManager.start();
    }

    private void createTestFolders() throws IOException {
        Files.createDirectories(fileFolder1);
        Files.createDirectories(fileFolder2);
    }

    @Test
    void startAndClose() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    void addAndRecoverFile() throws Exception {
        byte[] data = new byte[5 * 1024 * 1024];
        random.nextBytes(data);
        Files.write(testFile1, data);

        Thread.sleep(1500);

        backupManager.recoverFile(testFile1, recoveryDest);
        byte[] recoveredFile = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data, recoveredFile);
    }

    @Test
    void addAndRecoverFileVersion() throws Exception {
        byte[] data1 = new byte[5 * 1024 * 1024];
        random.nextBytes(data1);
        Files.write(testFile1, data1);
        DateTime dateTime1 = DateTime.now();

        Thread.sleep(1500);

        byte[] data2 = new byte[5 * 1024 * 1024];
        random.nextBytes(data2);
        Files.write(testFile1, data2);
        DateTime dateTime2 = DateTime.now();

        Thread.sleep(1500);

        backupManager.recoverFile(testFile1, recoveryDest, dateTime1);
        byte[] recoveredFile1 = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data1, recoveredFile1);

        backupManager.recoverFile(testFile1, recoveryDest, dateTime2);
        byte[] recoveredFile2 = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data2, recoveredFile2);
    }

    @Test
    void addAndRecoverFileVersionReboot() throws Exception {
        byte[] data1 = new byte[5 * 1024 * 1024];
        random.nextBytes(data1);
        Files.write(testFile1, data1);
        DateTime dateTime1 = DateTime.now();

        Thread.sleep(1500);

        byte[] data2 = new byte[5 * 1024 * 1024];
        random.nextBytes(data2);
        Files.write(testFile1, data2);
        DateTime dateTime2 = DateTime.now();

        Thread.sleep(1500);

        backupManager.close();

        backupManager = new BackupManager(config, configPath);

        backupManager.recoverFile(testFile1, recoveryDest, dateTime1);
        byte[] recoveredFile1 = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data1, recoveredFile1);

        backupManager.recoverFile(testFile1, recoveryDest, dateTime2);
        byte[] recoveredFile2 = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data2, recoveredFile2);
    }

    @Test
    void addAndRecoverFileVersionOverflowTest() throws Exception {
        byte[] data5 = new byte[5 * 1024 * 1024];
        random.nextBytes(data5);
        Files.write(testFile2, data5);
        DateTime dateTime5 = DateTime.now();

        byte[] data1 = new byte[5 * 1024 * 1024];
        random.nextBytes(data1);
        Files.write(testFile1, data1);
        DateTime dateTime1 = DateTime.now();

        Thread.sleep(1500);

        byte[] data2 = new byte[5 * 1024 * 1024];
        random.nextBytes(data2);
        Files.write(testFile1, data2);
        DateTime dateTime2 = DateTime.now();

        Thread.sleep(1500);

        byte[] data3 = new byte[5 * 1024 * 1024];
        random.nextBytes(data3);
        Files.write(testFile1, data3);
        DateTime dateTime3 = DateTime.now();

        byte[] data4 = new byte[5 * 1024 * 1024];
        random.nextBytes(data4);
        Files.write(testFile3, data4);
        DateTime dateTime4 = DateTime.now();

        Thread.sleep(1500);

        backupManager.close();

        backupManager = new BackupManager(config, configPath);
        backupManager.start();

        backupManager.recoverFile(testFile1, recoveryDest, dateTime1);
        byte[] recoveredFile1 = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data1, recoveredFile1);

        backupManager.trimStorage();

        assertThrows(StorageManagerException.class, () -> backupManager.recoverFile(testFile1, recoveryDest, dateTime1));
        assertThrows(StorageManagerException.class, () -> backupManager.recoverFile(testFile1, recoveryDest, dateTime2));

        backupManager.recoverFile(testFile1, recoveryDest, dateTime3);
        byte[] recoveredFile3 = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data3, recoveredFile3);

        backupManager.recoverFile(testFile3, recoveryDest, dateTime4);
        byte[] recoveredFile4 = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data4, recoveredFile4);

        backupManager.recoverFile(testFile2, recoveryDest, dateTime5);
        byte[] recoveredFile5 = Files.readAllBytes(recoveryDest);
        Assertions.assertArrayEquals(data5, recoveredFile5);

        Thread.sleep(1500);
    }

    @AfterEach
    void tearDown() throws Exception {
        backupManager.close();

        deleteAllTestFilesAndFolders();
        Files.deleteIfExists(fileBaseFolder);
        Files.deleteIfExists(configPath);

        Files.deleteIfExists(recoveryDest);
    }

    private void deleteAllTestFilesAndFolders() throws IOException {
        Files.deleteIfExists(testFile1);
        Files.deleteIfExists(testFile2);
        Files.deleteIfExists(testFile3);
        Files.deleteIfExists(fileFolder1);
        Files.deleteIfExists(fileFolder2);
        Files.deleteIfExists(fileFolder2.getParent());
    }

    static Path getBaseFolder() throws Exception {
        String baseDir = System.getProperty("java.io.tmpdir") + File.separator + "membrane" + File.separator + "full~,~test";
        Files.createDirectories(Paths.get(baseDir));
        String tmpDir = baseDir;
        Path tmpPath = Paths.get(tmpDir);
        while(Files.exists(tmpPath, LinkOption.NOFOLLOW_LINKS)) {
            tmpDir = baseDir + File.separator + new BigInteger(32, new SecureRandom()).toString(32);
            tmpPath = Paths.get(tmpDir);
        }
        Files.createDirectory(tmpPath);
        return Paths.get(tmpDir);
    }
}