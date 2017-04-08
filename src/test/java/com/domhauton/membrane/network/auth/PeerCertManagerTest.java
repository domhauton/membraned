package com.domhauton.membrane.network.auth;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

/**
 * Created by dominic on 07/04/17.
 */
class PeerCertManagerTest {

  private String testFolder;
  private PeerCertManager peerCertManager;

  private MembraneAuthInfo authInfo1;
  private MembraneAuthInfo authInfo2;
  private MembraneAuthInfo authInfo3;

  private ContractManager mockContractManager;

  @BeforeEach
  void setUp() throws Exception {
    AuthUtils.addProvider();
    testFolder = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    peerCertManager = new PeerCertManager(Paths.get(testFolder));
    authInfo1 = AuthUtils.generateAuthenticationInfo();
    authInfo2 = AuthUtils.generateAuthenticationInfo();
    authInfo3 = AuthUtils.generateAuthenticationInfo();

    mockContractManager = Mockito.mock(ContractManager.class);
  }

  @Test
  void basicStoreTest() {
    peerCertManager.addCertificate(authInfo1.getClientId(), authInfo1.getX509Certificate());
    Assertions.assertEquals(authInfo1.getX509Certificate(), peerCertManager.getCertificate(authInfo1.getClientId()));
  }

  @Test
  void basicPersistTest() {
    peerCertManager.addCertificate(authInfo1.getClientId(), authInfo1.getX509Certificate());
    peerCertManager = new PeerCertManager(Paths.get(testFolder));
    Assertions.assertEquals(authInfo1.getX509Certificate(), peerCertManager.getCertificate(authInfo1.getClientId()));
  }

  @Test
  void multiplePersistTest() {
    peerCertManager.addCertificate(authInfo1.getClientId(), authInfo1.getX509Certificate());
    peerCertManager.addCertificate(authInfo2.getClientId(), authInfo2.getX509Certificate());
    peerCertManager.addCertificate(authInfo3.getClientId(), authInfo3.getX509Certificate());
    peerCertManager = new PeerCertManager(Paths.get(testFolder));
    Assertions.assertEquals(authInfo1.getX509Certificate(), peerCertManager.getCertificate(authInfo1.getClientId()));
    Assertions.assertEquals(authInfo2.getX509Certificate(), peerCertManager.getCertificate(authInfo2.getClientId()));
    Assertions.assertEquals(authInfo3.getX509Certificate(), peerCertManager.getCertificate(authInfo3.getClientId()));
  }

  @Test
  void multiplePersistTrimTest() {
    peerCertManager.addCertificate(authInfo1.getClientId(), authInfo1.getX509Certificate());
    peerCertManager.addCertificate(authInfo2.getClientId(), authInfo2.getX509Certificate());
    peerCertManager = new PeerCertManager(Paths.get(testFolder));
    Assertions.assertEquals(authInfo1.getX509Certificate(), peerCertManager.getCertificate(authInfo1.getClientId()));
    Assertions.assertEquals(authInfo2.getX509Certificate(), peerCertManager.getCertificate(authInfo2.getClientId()));
    peerCertManager = new PeerCertManager(Paths.get(testFolder));

    Mockito.when(mockContractManager.getContractedPeers()).thenReturn(Collections.singleton(authInfo3.getClientId()));
    peerCertManager.setContractManager(mockContractManager);

    Assertions.assertEquals(authInfo1.getX509Certificate(), peerCertManager.getCertificate(authInfo1.getClientId()));
    Assertions.assertEquals(authInfo2.getX509Certificate(), peerCertManager.getCertificate(authInfo2.getClientId()));
    peerCertManager.addCertificate(authInfo3.getClientId(), authInfo3.getX509Certificate());
    Assertions.assertEquals(authInfo3.getX509Certificate(), peerCertManager.getCertificate(authInfo3.getClientId()));
    Assertions.assertThrows(NoSuchElementException.class, () -> peerCertManager.getCertificate(authInfo1.getClientId()));
    Assertions.assertThrows(NoSuchElementException.class, () -> peerCertManager.getCertificate(authInfo2.getClientId()));
  }

  @AfterEach
  void tearDown() throws IOException {
    File[] files = Paths.get(testFolder).toFile().listFiles();
    if (files != null) {
      Assertions.assertTrue(Arrays.stream(files).allMatch(File::delete));
    }
    Files.delete(Paths.get(testFolder));
  }
}