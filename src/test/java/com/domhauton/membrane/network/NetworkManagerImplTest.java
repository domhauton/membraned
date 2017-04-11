package com.domhauton.membrane.network;

import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by dominic on 11/04/17.
 */
class NetworkManagerImplTest {

  private String testFolder1;
  private Path testFolderPath1;
  private String testFolder2;
  private Path testFolderPath2;

  private static final int PORT_INTERNAL_1 = 14100;
  private static final int PORT_EXTERNAL_1 = 14105;
  private static final int PORT_INTERNAL_2 = 14200;
  private static final int PORT_EXTERNAL_2 = 14205;

  private NetworkManager networkManager1;
  private EvictingContractManager evictingContractManager1;
  private NetworkManager networkManager2;
  private EvictingContractManager evictingContractManager2;


  @BeforeEach
  void setUp() throws Exception {
    testFolder1 = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    testFolderPath1 = Paths.get(testFolder1);
    networkManager1 = new NetworkManagerImpl(testFolderPath1, PORT_INTERNAL_1, PORT_EXTERNAL_1);

    testFolder2 = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    testFolderPath2 = Paths.get(testFolder2);
    networkManager2 = new NetworkManagerImpl(testFolderPath2, PORT_INTERNAL_2, PORT_EXTERNAL_2);

    evictingContractManager1 = new EvictingContractManager(3);
    evictingContractManager2 = new EvictingContractManager(3);
  }

  @Test
  void runAttempt() throws Exception {

    networkManager1.run();
    Thread.sleep(1000);
  }

  @Test
  void testConnection() throws Exception {
    // Inject PEX entry for Peer 2 into Peer 1
    // Add Peer2 as a contract to Peer1
    String peerID2 = extractMembraneAuthInfo(networkManager2).getClientId();
    networkManager1.setContractManager(evictingContractManager1);
    evictingContractManager1.addContractedPeer(peerID2);
    extractPexManager(networkManager1).addEntry(peerID2, "127.0.0.1", PORT_INTERNAL_2, false, DateTime.now(), new byte[0]);

    // Run Peer1
    networkManager1.run();
    // Should attempt to connect to Peer 2;
    boolean peer2Connected = false;
    for (int i = 0; i < 10 && !peer2Connected; i++) {
      Thread.sleep(50);
      peer2Connected = networkManager1.peerConnected(peerID2);
    }
    Assertions.assertTrue(peer2Connected);

    networkManager1.close();
  }

  @Test
  void reconnectFromSelfGeneratedPex() throws Exception {
    // Inject PEX entry for Peer 2 into Peer 1
    // Add Peer2 as a contract to Peer1
    String peerID1 = extractMembraneAuthInfo(networkManager1).getClientId();
    String peerID2 = extractMembraneAuthInfo(networkManager2).getClientId();
    networkManager1.setContractManager(evictingContractManager1);
    evictingContractManager1.addContractedPeer(peerID2);
    extractPexManager(networkManager1).addEntry(peerID2, "127.0.0.1", PORT_INTERNAL_2, false, DateTime.now(), new byte[0]);

    // Set a contract manager but nothing else
    networkManager2.setContractManager(evictingContractManager2);
    evictingContractManager2.addContractedPeer(peerID1);

    // Run Peer1
    networkManager1.run();
    // Should attempt to connect to Peer 2;
    boolean peer2Connected = false;
    for (int i = 0; i < 10 && !peer2Connected; i++) {
      Thread.sleep(50);
      peer2Connected = networkManager1.peerConnected(peerID2);
    }
    Assertions.assertTrue(peer2Connected);

    // Give them a second to exchange PEX info
    Thread.sleep(100);

    networkManager2.close();

    // Now reboot both hosts. If peer 2 is run it should auto reconnect...

    networkManager2 = new NetworkManagerImpl(testFolderPath2, PORT_INTERNAL_2, PORT_EXTERNAL_2);
    networkManager2.setContractManager(evictingContractManager2);

    networkManager2.run();

    boolean peer1Reconnected = false;
    for (int i = 0; i < 10 && !peer1Reconnected; i++) {
      Thread.sleep(50);
      peer1Reconnected = networkManager2.peerConnected(peerID1);
    }
    Assertions.assertTrue(peer1Reconnected);
  }

  @AfterEach
  void tearDown() throws Exception {
    networkManager1.close();
    networkManager2.close();
    StorageManagerTestUtils.deleteDirectoryRecursively(testFolderPath1);
    StorageManagerTestUtils.deleteDirectoryRecursively(testFolderPath2);
  }

  private PexManager extractPexManager(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field pexField = networkManagerImpl.getClass().getDeclaredField("pexManager");
    pexField.setAccessible(true);
    return (PexManager) pexField.get(networkManagerImpl);
  }

  private MembraneAuthInfo extractMembraneAuthInfo(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field pexField = networkManagerImpl.getClass().getDeclaredField("membraneAuthInfo");
    pexField.setAccessible(true);
    return (MembraneAuthInfo) pexField.get(networkManagerImpl);
  }

  private Gatekeeper extractGatekeeper(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field pexField = networkManagerImpl.getClass().getDeclaredField("gatekeeper");
    pexField.setAccessible(true);
    return (Gatekeeper) pexField.get(networkManagerImpl);
  }

  private void injectNewStartTime(NetworkManager networkManager, DateTime newStartTime) throws Exception {
    Gatekeeper gatekeeper = extractGatekeeper(networkManager);
    Field startField = gatekeeper.getClass().getDeclaredField("startUpDateTime");
    startField.setAccessible(true);
    startField.set(gatekeeper, newStartTime);
  }
}