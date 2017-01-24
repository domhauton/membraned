package com.domhauton.membrane.prospector;

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

        while(Files.exists(testPath, LinkOption.NOFOLLOW_LINKS)) {
            testDir = testBaseDir + sep + new BigInteger(130, new SecureRandom()).toString(32);
            testPath = Paths.get(testDir);
        }

        logger.info("Using path {}", testPath);

        prospector = new Prospector(testDir);
        Files.createDirectory(testPath);
    }

    private void createTestFiles(String baseDir) {
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

    private void modifyTestFiles(String baseDir) {
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


    private void removeTestFiles(String baseDir) {
        IntStream.range(1, 10).boxed()
                .map(Object::toString)
                .map(num -> Paths.get(baseDir + sep + num))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    @Test
    void testFileDetection() throws Exception {
        Assertions.assertEquals(0, prospector.getFiles().size());
        createTestFiles(testDir);
        Assertions.assertEquals(9, prospector.getFiles().size());
        removeTestFiles(testDir);
    }

    @Test
    @DisplayName("Detecting a file that has been deleted")
    void testAddDeleteDetection() {
        Assertions.assertEquals(0, prospector.getFiles().size());
        Assertions.assertEquals(0, prospector.modifiedFiles().size());
        createTestFiles(testDir);
        Assertions.assertEquals(9, prospector.modifiedFiles().size());
        Assertions.assertEquals(0, prospector.modifiedFiles().size());
        removeTestFiles(testDir);
        Assertions.assertEquals(9, prospector.modifiedFiles().size());
        Assertions.assertEquals(0, prospector.modifiedFiles().size());
    }

    @Test
    @DisplayName("Detecting a file that has been modified")
    void testAddModifyDetection() {
        Assertions.assertEquals(0, prospector.getFiles().size());
        Assertions.assertEquals(0, prospector.modifiedFiles().size());
        createTestFiles(testDir);
        Assertions.assertEquals(9, prospector.modifiedFiles().size());
        Assertions.assertEquals(0, prospector.modifiedFiles().size());
        modifyTestFiles(testDir);
        Assertions.assertEquals(4, prospector.modifiedFiles().size());
        Assertions.assertEquals(0, prospector.modifiedFiles().size());
        removeTestFiles(testDir);
        Assertions.assertEquals(9, prospector.modifiedFiles().size());
        Assertions.assertEquals(0, prospector.modifiedFiles().size());
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.delete(testPath);
    }
}