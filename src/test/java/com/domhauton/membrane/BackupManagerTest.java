package com.domhauton.membrane;

import com.domhauton.membrane.config.Config;
import com.domhauton.membrane.config.ConfigException;
import com.domhauton.membrane.config.items.*;
import com.domhauton.membrane.config.items.data.WatchFolder;
import com.domhauton.membrane.storage.StorageManagerException;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
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
import java.util.LinkedList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

  private WatchFolder watchFolder1;
  private WatchFolder watchFolder2;

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

    watchFolder1 = new WatchFolder(fileFolder1.toString(), false);
    watchFolder2 = new WatchFolder(fileFolder2.getParent().toString(), true);

    config = new Config(
        new ContractManagerConfig(true, 10, true),
        new NetworkConfig(),
        new StorageConfig(
            basePath.toString() + File.separator + "storage" + File.separator + "localShard",
            basePath.toString() + File.separator + "storage" + File.separator + "peerBlock",
            1,
            (int) ((19.0d / 0.2d) / 0.8d)),
        new FileWatcherConfig(4,
            new LinkedList<>(Arrays.asList(watchFolder1, watchFolder2)),
            1,
            1),
        new RestAPIConfig(13200));

    backupManager = new BackupManager(config, configPath);
    backupManager.run();
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
  void forceFalseStartByLaunchingTwice() throws Exception {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new BackupManager(config, configPath));
  }

  @Test
  void addAndRecoverFile() throws Exception {
    createTestFolders();
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
    createTestFolders();
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

    Files.delete(recoveryDest);

    backupManager.recoverFile(testFile1, recoveryDest, dateTime2);
    byte[] recoveredFile2 = Files.readAllBytes(recoveryDest);
    Assertions.assertArrayEquals(data2, recoveredFile2);
  }

  @Test
  void addAndRecoverFileVersionReboot() throws Exception {
    createTestFolders();
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

    Files.delete(recoveryDest);

    backupManager.recoverFile(testFile1, recoveryDest, dateTime2);
    byte[] recoveredFile2 = Files.readAllBytes(recoveryDest);
    Assertions.assertArrayEquals(data2, recoveredFile2);
  }

  @Test
  void reactionToNoFolder() throws Exception {
    Thread.sleep(1500);

    createTestFolders();

    Thread.sleep(1500);

    byte[] data1 = new byte[5 * 1024 * 1024];
    random.nextBytes(data1);
    Files.write(testFile1, data1);
    DateTime dateTime1 = DateTime.now();

    Thread.sleep(1500);

    deleteAllTestFilesAndFolders();

    Thread.sleep(1500);

    Assertions.assertNotEquals(backupManager.getCurrentFiles(), backupManager.getReferencedFiles());

    createTestFolders();

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

    Files.delete(recoveryDest);

    backupManager.recoverFile(testFile1, recoveryDest, dateTime2);
    byte[] recoveredFile2 = Files.readAllBytes(recoveryDest);
    Assertions.assertArrayEquals(data2, recoveredFile2);
  }

  @Test
  void addAndRecoverFileVersionOverflowTest() throws Exception {
    createTestFolders();
    byte[] data5 = new byte[4 * 1024 * 1024];
    random.nextBytes(data5);
    Files.write(testFile2, data5);
    DateTime dateTime5 = DateTime.now();

    byte[] data1 = new byte[4 * 1024 * 1024];
    random.nextBytes(data1);
    Files.write(testFile1, data1);
    DateTime dateTime1 = DateTime.now();

    Thread.sleep(1500);

    byte[] data2 = new byte[4 * 1024 * 1024];
    random.nextBytes(data2);
    Files.write(testFile1, data2);
    DateTime dateTime2 = DateTime.now();

    Thread.sleep(1500);

    byte[] data3 = new byte[4 * 1024 * 1024];
    random.nextBytes(data3);
    Files.write(testFile1, data3);
    DateTime dateTime3 = DateTime.now();

    byte[] data4 = new byte[4 * 1024 * 1024];
    random.nextBytes(data4);
    Files.write(testFile3, data4);
    DateTime dateTime4 = DateTime.now();

    Thread.sleep(1500);

    Assertions.assertEquals(3, backupManager.getFileHistory(testFile1).size());

    backupManager.close();

    backupManager = new BackupManager(config, configPath);
    backupManager.run();

    Assertions.assertEquals(backupManager.getCurrentFiles().size(), backupManager.getReferencedFiles().size());

    backupManager.recoverFile(testFile1, recoveryDest, dateTime1);
    byte[] recoveredFile1 = Files.readAllBytes(recoveryDest);
    Assertions.assertArrayEquals(data1, recoveredFile1);

    Files.delete(recoveryDest);

    Assertions.assertEquals(5 * 4 * 1024 * 1024, backupManager.getStorageSize());
    backupManager.trimStorage();
    Assertions.assertEquals(4 * 4 * 1024 * 1024, backupManager.getStorageSize());

    assertThrows(StorageManagerException.class, () -> backupManager.recoverFile(testFile1, recoveryDest, dateTime1));

    backupManager.recoverFile(testFile1, recoveryDest, dateTime2);
    byte[] recoveredFile2 = Files.readAllBytes(recoveryDest);
    Assertions.assertArrayEquals(data2, recoveredFile2);

    Files.delete(recoveryDest);

    backupManager.recoverFile(testFile1, recoveryDest, dateTime3);
    byte[] recoveredFile3 = Files.readAllBytes(recoveryDest);
    Assertions.assertArrayEquals(data3, recoveredFile3);

    Files.delete(recoveryDest);

    backupManager.recoverFile(testFile3, recoveryDest, dateTime4);
    byte[] recoveredFile4 = Files.readAllBytes(recoveryDest);
    Assertions.assertArrayEquals(data4, recoveredFile4);

    Files.delete(recoveryDest);

    backupManager.recoverFile(testFile2, recoveryDest, dateTime5);
    byte[] recoveredFile5 = Files.readAllBytes(recoveryDest);
    Assertions.assertArrayEquals(data5, recoveredFile5);
  }

  @Test
  void watchFolderConfigurationTest() throws Exception {
    createTestFolders();
    Thread.sleep(1200);

    Assertions.assertTrue(backupManager.getConfig().getFileWatcher().getFolders().contains(watchFolder1));
    Assertions.assertTrue(backupManager.getConfig().getFileWatcher().getFolders().contains(watchFolder2));
    System.out.println(backupManager.getWatchedFolders().toString());
    Assertions.assertTrue(backupManager.getWatchedFolders().contains(watchFolder1.getDirectory()));
    Assertions.assertTrue(backupManager.getWatchedFolders().contains(watchFolder2.getDirectory()));

    backupManager.removeWatchFolder(watchFolder1);

    Thread.sleep(1200);

    Assertions.assertFalse(backupManager.getConfig().getFileWatcher().getFolders().contains(watchFolder1));
    Assertions.assertTrue(backupManager.getConfig().getFileWatcher().getFolders().contains(watchFolder2));
    Assertions.assertFalse(backupManager.getWatchedFolders().contains(watchFolder1.getDirectory()));
    Assertions.assertTrue(backupManager.getWatchedFolders().contains(watchFolder2.getDirectory()));

    Assertions.assertThrows(IllegalArgumentException.class, () -> backupManager.removeWatchFolder(watchFolder1));
    backupManager.addWatchFolder(watchFolder1);

    Thread.sleep(1200);

    Assertions.assertTrue(backupManager.getConfig().getFileWatcher().getFolders().contains(watchFolder1));
    Assertions.assertTrue(backupManager.getConfig().getFileWatcher().getFolders().contains(watchFolder2));
    Assertions.assertTrue(backupManager.getWatchedFolders().contains(watchFolder1.getDirectory()));
    Assertions.assertTrue(backupManager.getWatchedFolders().contains(watchFolder2.getDirectory()));

    Assertions.assertThrows(IllegalArgumentException.class, () -> backupManager.addWatchFolder(watchFolder1));

    Assertions.assertTrue(backupManager.getConfigPath().toFile().setWritable(false));
    Assertions.assertThrows(ConfigException.class, () -> backupManager.removeWatchFolder(watchFolder1));
    Assertions.assertThrows(ConfigException.class, () -> backupManager.addWatchFolder(watchFolder1));
    Assertions.assertTrue(backupManager.getConfigPath().toFile().setWritable(true));
    Files.delete(backupManager.getConfigPath());
  }

  @Test
  void backupManagerInfoTest() throws Exception {
    Assertions.assertFalse(backupManager.isMonitorMode());
    Assertions.assertTrue(new Duration(backupManager.getStartTime(), DateTime.now()).toStandardSeconds().getSeconds() < 10);
  }

  @AfterEach
  void tearDown() throws Exception {
    backupManager.close();

    deleteAllTestFilesAndFolders();
    StorageManagerTestUtils.deleteDirectoryRecursively(basePath);
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

  private static Path getBaseFolder() throws Exception {
    String baseDir = System.getProperty("java.io.tmpdir") + File.separator + "membrane" + File.separator + "full~,~test";
    Files.createDirectories(Paths.get(baseDir));
    String tmpDir = baseDir;
    Path tmpPath = Paths.get(tmpDir);
    while (Files.exists(tmpPath, LinkOption.NOFOLLOW_LINKS)) {
      tmpDir = baseDir + File.separator + new BigInteger(32, new SecureRandom()).toString(32);
      tmpPath = Paths.get(tmpDir);
    }
    Files.createDirectory(tmpPath);
    return Paths.get(tmpDir);
  }
}