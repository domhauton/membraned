package com.domhauton.membrane.prospector;

import com.domhauton.membrane.config.items.WatchFolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.domhauton.membrane.prospector.ProspectorTestUtils.*;


/**
 * Created by dominic on 23/01/17.
 */
class ProspectorTest {

  private Logger logger = LogManager.getLogger();

  private Prospector prospector;

  private WatchFolder watchFolderRec;
  private WatchFolder watchFolderNonRec;

  private String testBaseDir;
  private String testDir;
  private Path testPath;
  private String sep;


  @BeforeEach
  void setUp() throws Exception {
    sep = java.nio.file.FileSystems.getDefault().getSeparator();

    testBaseDir = System.getProperty("java.io.tmpdir") + sep + "membrane";
    testPath = Paths.get(testBaseDir);
    if (Files.notExists(testPath)) {
      Files.createDirectory(testPath);
    }

    testDir = createRandomFolder(testBaseDir);
    testPath = Paths.get(testDir);

    logger.info("Using path {}", testPath);

    prospector = new Prospector();
    watchFolderRec = new WatchFolder(testDir, true);
    watchFolderNonRec = new WatchFolder(testDir, false);
  }


  @Test
  @DisplayName("Test basic file detection")
  void testFileDetection() throws Exception {
    prospector.addWatchFolder(watchFolderNonRec);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    removeTestFiles(testDir);
  }

  @Test
  @DisplayName("Detecting a file that has been deleted")
  void testAddDeleteDetection() throws Exception {
    prospector.addWatchFolder(watchFolderNonRec);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    removeTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getRemovedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
  }

  @Test
  @DisplayName("Detecting a file that has been modified")
  void testAddModifyDetection() throws Exception {
    prospector.addWatchFolder(watchFolderNonRec);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    modifyTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.MODIFIED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    removeTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getRemovedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getRemovedFiles().size());
  }

  @Test
  @DisplayName("Detect Recursive Files")
  void testAddModifyRecursive() throws Exception {
    String testRecDir = createRandomFolder(testDir);
    prospector.addWatchFolder(watchFolderRec);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testRecDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    modifyTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.MODIFIED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    removeTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getRemovedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getRemovedFiles().size());
    removeTestFiles(testRecDir);
    Files.delete(Paths.get(testRecDir));
  }

  @Test
  @DisplayName("Ignore Recursive Folders")
  void testAddModifyNonRecursive() throws Exception {
    String testRecDir = createRandomFolder(testDir);
    prospector.addWatchFolder(watchFolderNonRec);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testRecDir);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    modifyTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.MODIFIED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    removeTestFiles(testDir);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getRemovedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getRemovedFiles().size());
    removeTestFiles(testRecDir);
    Files.delete(Paths.get(testRecDir));
  }

  @Test
  @DisplayName("Test Wildcard Nonrecursive Folders")
  void testAddModifyNonRecursiveWildcard() throws Exception {
    String testRecDir = createRandomFolder(testDir);
    String testRecDirInner = createRandomFolder(testRecDir);
    WatchFolder watchFolder = new WatchFolder(testDir + sep + "*", false);
    prospector.addWatchFolder(watchFolder);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testDir);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testRecDir);
    createTestFiles(testRecDirInner);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT, prospector.checkChanges().getChangedFiles().size());
    removeTestFiles(testDir);
    Assertions.assertEquals(0, prospector.checkChanges().getRemovedFiles().size());
    removeTestFiles(testRecDirInner);
    removeTestFiles(testRecDir);
    Files.delete(Paths.get(testRecDirInner));
    Files.delete(Paths.get(testRecDir));
  }

  @Test
  @DisplayName("Test Wildcard Recursive Folders")
  void testAddModifyRecursiveWildcard() throws Exception {
    String testRecDir = createRandomFolder(testDir);
    String testRecDirInner = createRandomFolder(testRecDir);
    WatchFolder watchFolder = new WatchFolder(testDir + sep + "*", true);
    prospector.addWatchFolder(watchFolder);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testDir);
    Assertions.assertEquals(0, prospector.checkChanges().getChangedFiles().size());
    createTestFiles(testRecDir);
    createTestFiles(testRecDirInner);
    Assertions.assertEquals(ProspectorTestUtils.CREATED_FILES_COUNT * 2, prospector.checkChanges().getChangedFiles().size());
    removeTestFiles(testDir);
    Assertions.assertEquals(0, prospector.checkChanges().getRemovedFiles().size());
    removeTestFiles(testRecDirInner);
    removeTestFiles(testRecDir);
    Files.delete(Paths.get(testRecDirInner));
    Files.delete(Paths.get(testRecDir));
  }

  @AfterEach
  void tearDown() throws Exception {
    Files.delete(testPath);
  }
}