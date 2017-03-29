package com.domhauton.membrane.network.pex;

import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dominic on 29/03/17.
 */
class PexManagerTest {
  private static final String BASE_FOLDER = System.getProperty("tmp") + File.separator + "membrane";
  private String testFolder;

  private PexManager pexManager;

  private static final String PEER_1 = "PEER_HASH_1";
  private static final String PEER_2 = "PEER_HASH_2";
  private static final String PEER_3 = "PEER_HASH_3";
  private static final String PEER_4 = "PEER_HASH_4";

  private static final String IP_1 = "192.168.0.2";

  private static final int PORT_1 = 80;

  @BeforeEach
  void setUp() throws Exception {
    testFolder = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    pexManager = new PexManager(3, Paths.get(testFolder));
  }

  @Test
  void simpleTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false);
    PexEntry entry = pexManager.getEntry(PEER_1);
    Assertions.assertEquals(IP_1, entry.getAddress());
    Assertions.assertEquals(PORT_1, entry.getPort());
    Assertions.assertFalse(entry.isPublicEntry());
  }

  @Test
  void invalidEntryTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, -5, false);
    Assertions.assertThrows(PexException.class, () -> pexManager.getEntry(PEER_1));
  }

  @Test
  void overflowTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false);
    pexManager.addEntry(PEER_2, IP_1, PORT_1, false);
    pexManager.addEntry(PEER_3, IP_1, PORT_1, false);
    Assertions.assertIterableEquals(Arrays.asList(PEER_1, PEER_2, PEER_3), pexManager.getAvailablePexPeers());
    pexManager.addEntry(PEER_4, IP_1, PORT_1, false);
    Assertions.assertIterableEquals(Arrays.asList(PEER_2, PEER_3, PEER_4), pexManager.getAvailablePexPeers());
  }

  @Test
  void simpleTestFromFile() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false);
    pexManager.saveLedger();
    // RELOAD
    pexManager = new PexManager(3, Paths.get(testFolder));
    PexEntry entry = pexManager.getEntry(PEER_1);
    Assertions.assertEquals(IP_1, entry.getAddress());
    Assertions.assertEquals(PORT_1, entry.getPort());
    Assertions.assertFalse(entry.isPublicEntry());
  }

  @Test
  void simpleTestFromBackupFile() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false);
    pexManager.saveLedger();
    Assertions.assertFalse(getBackupPath().toFile().exists());
    Assertions.assertTrue(getNormalPath().toFile().exists());
    pexManager.saveLedger(); // Double save to test behaviour
    Assertions.assertFalse(getBackupPath().toFile().exists());
    Assertions.assertTrue(getNormalPath().toFile().exists());
    Files.copy(getNormalPath(), getBackupPath());
    // RELOAD
    pexManager = new PexManager(3, Paths.get(testFolder));
    PexEntry entry = pexManager.getEntry(PEER_1);
    Assertions.assertEquals(IP_1, entry.getAddress());
    Assertions.assertEquals(PORT_1, entry.getPort());
    Assertions.assertFalse(entry.isPublicEntry());
  }

  @Test
  void backupFileLockedTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false);
    pexManager.saveLedger();
    Files.move(getNormalPath(), getBackupPath());

    Set<PosixFilePermission> perms = new HashSet<>();
    Files.setPosixFilePermissions(getBackupPath(), perms);
    // RELOAD
    Assertions.assertThrows(PexException.class, () -> new PexManager(3, Paths.get(testFolder)));


    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.OWNER_WRITE);
    Files.setPosixFilePermissions(getNormalPath(), perms);

    pexManager = new PexManager(3, Paths.get(testFolder));
    PexEntry entry = pexManager.getEntry(PEER_1);
    Assertions.assertEquals(IP_1, entry.getAddress());
    Assertions.assertEquals(PORT_1, entry.getPort());
    Assertions.assertFalse(entry.isPublicEntry());

    Files.setPosixFilePermissions(Paths.get(testFolder), Collections.emptySet());
    Assertions.assertThrows(PexException.class, () -> pexManager.saveLedger());
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(Paths.get(testFolder), perms);
    Files.setPosixFilePermissions(getNormalPath(), perms);
  }

  @AfterEach
  void tearDown() throws Exception {
    Files.deleteIfExists(getNormalPath());
    Files.deleteIfExists(getBackupPath());
    Files.deleteIfExists(Paths.get(testFolder));
  }

  private Path getBackupPath() {
    return Paths.get(testFolder + File.separator + PexManager.PEX_BACKUP_FILE_NAME);
  }

  private Path getNormalPath() {
    return Paths.get(testFolder + File.separator + PexManager.PEX_FILE_NAME);
  }
}