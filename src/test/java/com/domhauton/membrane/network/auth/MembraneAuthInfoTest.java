package com.domhauton.membrane.network.auth;

import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.security.Security;
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
    AuthUtils.addProvider();
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

  @Test
  void genSaveFail() throws Exception {
    MembraneAuthInfo membraneAuthInfo = AuthUtils.generateAuthenticationInfo();
    membraneAuthInfo.write(innerPath);
    Assertions.assertThrows(IOException.class, () -> membraneAuthInfo.write(innerPath));
  }

  @Test
  void readFail() throws Exception {
    Assertions.assertThrows(AuthException.class, () -> AuthFileUtils.loadCertificate(Paths.get(innerPath + File.separator + "cert")));
    Assertions.assertThrows(AuthException.class, () -> AuthFileUtils.loadPrivateKey(Paths.get(innerPath + File.separator + "cert")));
    Assertions.assertThrows(AuthException.class, () -> AuthFileUtils.loadPublicKey(Paths.get(innerPath + File.separator + "private")));
  }

  @Test
  void removeSecurityProvider() throws Exception {
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    Assertions.assertThrows(AuthException.class, AuthUtils::generateAuthenticationInfo);
    AuthUtils.addProvider();
    AuthUtils.generateAuthenticationInfo();
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