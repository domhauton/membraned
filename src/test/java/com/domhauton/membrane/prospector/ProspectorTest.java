package com.domhauton.membrane.prospector;

import com.domhauton.membrane.config.items.WatchFolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.stream.IntStream;


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
        if(Files.notExists(testPath)) {
            Files.createDirectory(testPath);
        }

        testDir = createRandomFolder(testBaseDir);
        testPath = Paths.get(testDir);

        logger.info("Using path {}", testPath);

        prospector = new Prospector();
        watchFolderRec = new WatchFolder(testDir, true);
        watchFolderNonRec = new WatchFolder(testDir, false);
    }

    private String createRandomFolder(String baseDir) throws Exception {
        String tmpDir = baseDir;
        Path tmpPath = Paths.get(baseDir);
        while(Files.exists(tmpPath, LinkOption.NOFOLLOW_LINKS)) {
            tmpDir = baseDir + sep + new BigInteger(130, new SecureRandom()).toString(32);
            tmpPath = Paths.get(tmpDir);
        }
        Files.createDirectory(tmpPath);
        return tmpDir;
    }

    private void createTestFiles(String baseDir) throws Exception {
        IntStream.range(1, 10).boxed()
                .map(Object::toString)
                .map(num -> Paths.get(baseDir + sep + num))
                .forEach(path -> {
                    try {
                        Files.createFile(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private void modifyTestFiles(String baseDir) throws Exception {
        IntStream.range(1, 5).boxed()
                .map(Object::toString)
                .map(num -> Paths.get(baseDir + sep + num))
                .forEach(path -> {
                    try {
                        Files.write(path, "foobar".getBytes());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }


    private void removeTestFiles(String baseDir) throws Exception {
        IntStream.range(1, 10).boxed()
                .map(Object::toString)
                .map(num -> Paths.get(baseDir + sep + num))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    @Test
    @DisplayName("Test basic file detection")
    void testFileDetection() throws Exception {
        prospector.addFolder(watchFolderNonRec);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        removeTestFiles(testDir);
    }

    @Test
    @DisplayName("Detecting a file that has been deleted")
    void testAddDeleteDetection() throws Exception {
        prospector.addFolder(watchFolderNonRec);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        removeTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
    }

    @Test
    @DisplayName("Detecting a file that has been modified")
    void testAddModifyDetection() throws Exception {
        prospector.addFolder(watchFolderNonRec);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        modifyTestFiles(testDir);
        Assertions.assertEquals(4, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        removeTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
    }

    @Test
    @DisplayName("Detect Recursive Files")
    void testAddModifyRecursive() throws Exception {
        String testRecDir = createRandomFolder(testDir);
        prospector.addFolder(watchFolderRec);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testRecDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        modifyTestFiles(testDir);
        Assertions.assertEquals(4, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        removeTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        removeTestFiles(testRecDir);
        Files.delete(Paths.get(testRecDir));
    }

    @Test
    @DisplayName("Ignore Recursive Folders")
    void testAddModifyNonRecursive() throws Exception {
        String testRecDir = createRandomFolder(testDir);
        prospector.addFolder(watchFolderNonRec);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testRecDir);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        modifyTestFiles(testDir);
        Assertions.assertEquals(4, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        removeTestFiles(testDir);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        removeTestFiles(testRecDir);
        Files.delete(Paths.get(testRecDir));
    }

    @Test
    @DisplayName("Test Wildcard Nonrecursive Folders")
    void testAddModifyNonRecursiveWildcard() throws Exception {
        String testRecDir = createRandomFolder(testDir);
        String testRecDirInner = createRandomFolder(testRecDir);
        WatchFolder watchFolder = new WatchFolder(testDir + sep + "*", false);
        prospector.addFolder(watchFolder);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testDir);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testRecDir);
        createTestFiles(testRecDirInner);
        Assertions.assertEquals(9, prospector.checkChanges().size());
        removeTestFiles(testDir);
        Assertions.assertEquals(0, prospector.checkChanges().size());
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
        prospector.addFolder(watchFolder);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testDir);
        Assertions.assertEquals(0, prospector.checkChanges().size());
        createTestFiles(testRecDir);
        createTestFiles(testRecDirInner);
        Assertions.assertEquals(18, prospector.checkChanges().size());
        removeTestFiles(testDir);
        Assertions.assertEquals(0, prospector.checkChanges().size());
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