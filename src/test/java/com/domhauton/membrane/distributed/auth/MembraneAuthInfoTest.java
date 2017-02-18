package com.domhauton.membrane.distributed.auth;

import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Created by dominic on 13/02/17.
 */
class MembraneAuthInfoTest {
  private static final Logger logger = LogManager.getLogger();
  private final Path basePath = Paths.get(StorageManagerTestUtils.BASE_DIR);
  private Path innerPath;

  @BeforeEach
  void setUp() throws Exception {
    innerPath = Paths.get(StorageManagerTestUtils.createRandomFolder(basePath.toString()));
    logger.info("Testing in dir {}", innerPath);
  }

  @Test
  void generateInfo() throws Exception {
    MembraneAuthInfo membraneAuthInfo1 = AuthUtils.generateAuthenticationInfo();
    MembraneAuthInfo membraneAuthInfo2 = AuthUtils.generateAuthenticationInfo();
    Assertions.assertNotEquals(membraneAuthInfo1, membraneAuthInfo2);
  }

  @Test
  void genSaveAndLoad() throws Exception {
    MembraneAuthInfo membraneAuthInfo = AuthUtils.generateAuthenticationInfo();
    membraneAuthInfo.write(innerPath);
    MembraneAuthInfo membraneAuthInfoLoaded = new MembraneAuthInfo(innerPath);
    Assertions.assertEquals(membraneAuthInfo, membraneAuthInfoLoaded);
  }

  @AfterEach
  void tearDown() throws Exception {
    Path authPath = Paths.get(innerPath.toString() + MembraneAuthInfo.INNER_PATH);
    File[] fileList = authPath.toFile().listFiles();
    if (fileList != null) {
      Stream.of(fileList).filter(file -> !file.isDirectory()).forEach(file -> {
        try {
          Files.setPosixFilePermissions(file.toPath(), Collections.singleton(PosixFilePermission.OWNER_WRITE));
          Files.delete(file.toPath());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }

      });
    }
    Files.deleteIfExists(authPath);
    Files.deleteIfExists(innerPath);
  }
}