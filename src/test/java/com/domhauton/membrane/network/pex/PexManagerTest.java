package com.domhauton.membrane.network.pex;

import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.messages.PeerMessage;
import com.domhauton.membrane.network.messages.PexAdvertisement;
import com.domhauton.membrane.network.messages.PexQueryRequest;
import com.domhauton.membrane.network.upnp.ExternalAddress;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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
  private String testFolder;

  private PexManager pexManager;

  private static final String PEER_1 = "PEER_HASH_1";
  private static final String PEER_2 = "PEER_HASH_2";
  private static final String PEER_3 = "PEER_HASH_3";
  private static final String PEER_4 = "PEER_HASH_4";

  private static final String IP_1 = "192.168.0.2";
  private static final String IP_2 = "192.168.0.3";
  private static final String IP_3 = "192.168.0.4";

  private static final int PORT_1 = 80;
  private static final int PORT_2 = 81;
  private static final int PORT_3 = 82;

  private static final byte[] SIGNATURE_1 = "thisisasignature-asrtsrat".getBytes();

  private static final DateTime TEST_START_TIME = DateTime.now();

  @BeforeEach
  void setUp() throws Exception {
    testFolder = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    pexManager = new PexManager(3, Paths.get(testFolder));
  }

  @Test
  void simpleTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false, TEST_START_TIME, SIGNATURE_1);
    PexEntry entry = pexManager.getEntry(PEER_1);
    Assertions.assertEquals(IP_1, entry.getAddress());
    Assertions.assertEquals(PORT_1, entry.getPort());
    Assertions.assertFalse(entry.isPublicEntry());
  }

  @Test
  void invalidEntryTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, -5, false, TEST_START_TIME, SIGNATURE_1);
    Assertions.assertThrows(PexException.class, () -> pexManager.getEntry(PEER_1));
  }

  @Test
  void overflowTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false, TEST_START_TIME, SIGNATURE_1);
    pexManager.addEntry(PEER_2, IP_1, PORT_1, false, TEST_START_TIME, SIGNATURE_1);
    pexManager.addEntry(PEER_3, IP_1, PORT_1, false, TEST_START_TIME, SIGNATURE_1);
    Assertions.assertIterableEquals(Arrays.asList(PEER_1, PEER_2, PEER_3), pexManager.getAvailablePexPeers());
    pexManager.addEntry(PEER_4, IP_1, PORT_1, false, TEST_START_TIME, SIGNATURE_1);
    Assertions.assertIterableEquals(Arrays.asList(PEER_2, PEER_3, PEER_4), pexManager.getAvailablePexPeers());
  }

  @Test
  void publicTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, true, TEST_START_TIME, SIGNATURE_1);
    pexManager.addEntry(PEER_2, IP_2, PORT_2, true, TEST_START_TIME.plusSeconds(1), SIGNATURE_1);
    pexManager.addEntry(PEER_3, IP_3, PORT_3, false, TEST_START_TIME.plusSeconds(2), SIGNATURE_1);
    Assertions.assertIterableEquals(Arrays.asList(PEER_1, PEER_2, PEER_3), pexManager.getAvailablePexPeers());
    Set<PexEntry> actualPublicEntries = pexManager.getPublicEntries(1);
    for (PexEntry pexEntry : actualPublicEntries) {
      Assertions.assertEquals(IP_2, pexEntry.getAddress());
      Assertions.assertEquals(PORT_2, pexEntry.getPort());
    }
  }

  @Test
  void publicTwoEntriesTest() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, true, TEST_START_TIME, SIGNATURE_1);
    pexManager.addEntry(PEER_2, IP_2, PORT_2, true, TEST_START_TIME.plusSeconds(1), SIGNATURE_1);
    pexManager.addEntry(PEER_3, IP_3, PORT_3, false, TEST_START_TIME.plusSeconds(2), SIGNATURE_1);
    Assertions.assertIterableEquals(Arrays.asList(PEER_1, PEER_2, PEER_3), pexManager.getAvailablePexPeers());
    Set<PexEntry> actualPublicEntries = pexManager.getPublicEntries(2);
    for (PexEntry pexEntry : actualPublicEntries) {
      Assertions.assertTrue((pexEntry.getAddress().equals(IP_2) && pexEntry.getPort() == PORT_2) ||
          (pexEntry.getAddress().equals(IP_1) && pexEntry.getPort() == PORT_1));
    }
  }

  @Test
  void simpleTestFromFile() throws Exception {
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false, TEST_START_TIME, SIGNATURE_1);
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
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false, TEST_START_TIME, SIGNATURE_1);
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
    pexManager.addEntry(PEER_1, IP_1, PORT_1, false, TEST_START_TIME, SIGNATURE_1);
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

  @Test
  void connectToPublicPeersSingleTest() throws Exception {
    String ip1 = "192.168.0.1";
    String ip2 = "192.168.0.2";
    String ip3 = "192.168.0.3";
    int port1 = 80;
    int port2 = 81;
    int port3 = 82;
    pexManager.addUnconfirmedEntry(ip1, port1);
    Thread.sleep(10);
    pexManager.addUnconfirmedEntry(ip2, port2);
    Thread.sleep(10);
    pexManager.addUnconfirmedEntry(ip3, port3);

    ConnectionManager connectionManagerMock = Mockito.mock(ConnectionManager.class);

    pexManager.connectToPublicPeersInPex(connectionManagerMock, 1);

    Mockito.verify(connectionManagerMock, Mockito.atLeastOnce()).connectToPeer(ip3, port3);
    Mockito.verify(connectionManagerMock, Mockito.never()).connectToPeer(ip1, port1);
    Mockito.verify(connectionManagerMock, Mockito.never()).connectToPeer(ip2, port2);
  }

  @Test
  void connectToPublicPeersDoubleTest() throws Exception {
    String ip1 = "192.168.0.1";
    String ip2 = "192.168.0.2";
    String ip3 = "192.168.0.3";
    int port1 = 80;
    int port2 = 81;
    int port3 = 82;
    pexManager.addUnconfirmedEntry(ip1, port1);
    Thread.sleep(10);
    pexManager.addUnconfirmedEntry(ip2, port2);
    Thread.sleep(10);
    pexManager.addUnconfirmedEntry(ip3, port3);

    ConnectionManager connectionManagerMock = Mockito.mock(ConnectionManager.class);

    pexManager.connectToPublicPeersInPex(connectionManagerMock, 2);

    Mockito.verify(connectionManagerMock, Mockito.atLeastOnce()).connectToPeer(ip3, port3);
    Mockito.verify(connectionManagerMock, Mockito.never()).connectToPeer(ip1, port1);
    Mockito.verify(connectionManagerMock, Mockito.atLeastOnce()).connectToPeer(ip2, port2);
  }

  @Test
  void connectToPublicPeersDoubleOverwriteTest() throws Exception {
    String ip1 = "192.168.0.1";
    String ip2 = "192.168.0.2";
    String ip3 = "192.168.0.3";
    int port1 = 80;
    int port2 = 81;
    int port3 = 82;
    pexManager.addUnconfirmedEntry(ip1, port1);
    Thread.sleep(10);
    pexManager.addUnconfirmedEntry(ip2, port2);
    Thread.sleep(10);
    pexManager.addUnconfirmedEntry(ip3, port3);
    Thread.sleep(10);
    pexManager.addUnconfirmedEntry(ip1, port1);

    ConnectionManager connectionManagerMock = Mockito.mock(ConnectionManager.class);

    pexManager.connectToPublicPeersInPex(connectionManagerMock, 2);

    Mockito.verify(connectionManagerMock, Mockito.atLeastOnce()).connectToPeer(ip3, port3);
    Mockito.verify(connectionManagerMock, Mockito.atLeastOnce()).connectToPeer(ip1, port1);
    Mockito.verify(connectionManagerMock, Mockito.never()).connectToPeer(ip2, port2);
  }

  @Test
  void sendPexUpdatePublicTest() throws Exception {
    Peer peerMock1 = Mockito.mock(Peer.class);
    Peer peerMock2 = Mockito.mock(Peer.class);
    Set<Peer> peerMockSet = ImmutableSet.of(peerMock1, peerMock2);

    String ip1 = "192.168.0.1";
    int port1 = 80;

    ExternalAddress externalAddress1 = Mockito.mock(ExternalAddress.class);
    Mockito.when(externalAddress1.getIpAddress()).thenReturn(ip1);
    Mockito.when(externalAddress1.getPort()).thenReturn(port1);

    PexManager.sendPexUpdate(externalAddress1, peerMockSet, true);

    ArgumentCaptor<PeerMessage> peerMessageCaptor = ArgumentCaptor.forClass(PeerMessage.class);

    Mockito.verify(peerMock1, Mockito.times(1)).sendPeerMessage(peerMessageCaptor.capture());
    Mockito.verify(peerMock2, Mockito.times(1)).sendPeerMessage(peerMessageCaptor.capture());

    for (PeerMessage peerMessage : peerMessageCaptor.getAllValues()) {
      PexAdvertisement pexAdvertisement = (PexAdvertisement) peerMessage;
      Assertions.assertEquals(ip1, pexAdvertisement.getIp());
      Assertions.assertEquals(port1, pexAdvertisement.getPort());
      Assertions.assertEquals(true, pexAdvertisement.isPublicInfo());
    }
  }

  @Test
  void sendPexUpdateNonPublicTest() throws Exception {
    Peer peerMock1 = Mockito.mock(Peer.class);
    Peer peerMock2 = Mockito.mock(Peer.class);
    Set<Peer> peerMockSet = ImmutableSet.of(peerMock1, peerMock2);

    String ip1 = "192.168.1.1";
    int port1 = 82;

    ExternalAddress externalAddress1 = Mockito.mock(ExternalAddress.class);
    Mockito.when(externalAddress1.getIpAddress()).thenReturn(ip1);
    Mockito.when(externalAddress1.getPort()).thenReturn(port1);

    PexManager.sendPexUpdate(externalAddress1, peerMockSet, false);

    ArgumentCaptor<PeerMessage> peerMessageCaptor = ArgumentCaptor.forClass(PeerMessage.class);

    Mockito.verify(peerMock1, Mockito.times(1)).sendPeerMessage(peerMessageCaptor.capture());
    Mockito.verify(peerMock2, Mockito.times(1)).sendPeerMessage(peerMessageCaptor.capture());

    for (PeerMessage peerMessage : peerMessageCaptor.getAllValues()) {
      PexAdvertisement pexAdvertisement = (PexAdvertisement) peerMessage;
      Assertions.assertEquals(ip1, pexAdvertisement.getIp());
      Assertions.assertEquals(port1, pexAdvertisement.getPort());
      Assertions.assertEquals(false, pexAdvertisement.isPublicInfo());
    }
  }

  @Test
  void sendPexRequestPublicTest() throws Exception {
    Peer peerMock1 = Mockito.mock(Peer.class);
    Peer peerMock2 = Mockito.mock(Peer.class);
    Mockito.when(peerMock1.getUid()).thenReturn(PEER_1);
    Mockito.when(peerMock2.getUid()).thenReturn(PEER_2);
    Set<Peer> peerMockSet = ImmutableSet.of(peerMock1, peerMock2);

    Set<String> contractedPeerSet = ImmutableSet.of(PEER_1, PEER_2, PEER_3, PEER_4);

    PexManager.requestPexInformation(peerMockSet, contractedPeerSet, true);

    ArgumentCaptor<PeerMessage> peerMessageCaptor = ArgumentCaptor.forClass(PeerMessage.class);

    Mockito.verify(peerMock1, Mockito.times(1)).sendPeerMessage(peerMessageCaptor.capture());
    Mockito.verify(peerMock2, Mockito.times(1)).sendPeerMessage(peerMessageCaptor.capture());

    Set<String> expectedPeerRequest = ImmutableSet.of(PEER_3, PEER_4);

    for (PeerMessage peerMessage : peerMessageCaptor.getAllValues()) {
      PexQueryRequest pexQueryRequest = (PexQueryRequest) peerMessage;
      Assertions.assertEquals(expectedPeerRequest, pexQueryRequest.getRequestedPeers());
      Assertions.assertEquals(true, pexQueryRequest.isRequestPublic());
    }
  }

  @Test
  void sendPexRequestNonPublicTest() throws Exception {
    Peer peerMock1 = Mockito.mock(Peer.class);
    Peer peerMock2 = Mockito.mock(Peer.class);
    Mockito.when(peerMock1.getUid()).thenReturn(PEER_1);
    Mockito.when(peerMock2.getUid()).thenReturn("UNKNOWN PEER");
    Set<Peer> peerMockSet = ImmutableSet.of(peerMock1, peerMock2);

    Set<String> contractedPeerSet = ImmutableSet.of(PEER_1, PEER_2, PEER_3, PEER_4);

    PexManager.requestPexInformation(peerMockSet, contractedPeerSet, false);

    ArgumentCaptor<PeerMessage> peerMessageCaptor = ArgumentCaptor.forClass(PeerMessage.class);

    Mockito.verify(peerMock1, Mockito.times(1)).sendPeerMessage(peerMessageCaptor.capture());
    Mockito.verify(peerMock2, Mockito.times(1)).sendPeerMessage(peerMessageCaptor.capture());

    Set<String> expectedPeerRequest = ImmutableSet.of(PEER_2, PEER_3, PEER_4);

    for (PeerMessage peerMessage : peerMessageCaptor.getAllValues()) {
      PexQueryRequest pexQueryRequest = (PexQueryRequest) peerMessage;
      Assertions.assertEquals(expectedPeerRequest, pexQueryRequest.getRequestedPeers());
      Assertions.assertEquals(false, pexQueryRequest.isRequestPublic());
    }
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