package com.domhauton.membrane.network;

import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.tracker.Tracker;
import com.domhauton.membrane.network.tracker.TrackerManager;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import com.google.common.collect.ImmutableSet;
import org.bouncycastle.util.encoders.Base64;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

/**
 * Created by dominic on 11/04/17.
 */
class NetworkManagerImplTest {
  public final static Random RANDOM = new SecureRandom();

  private String testFolder1;
  private Path testFolderPath1;
  private String testFolder2;
  private Path testFolderPath2;
  private String testFolder3;
  private Path testFolderPath3;

  private static final int PORT_INTERNAL_1 = 14100;
  private static final int PORT_EXTERNAL_1 = 14105;
  private static final int PORT_INTERNAL_2 = 14200;
  private static final int PORT_EXTERNAL_2 = 14205;
  private static final int PORT_INTERNAL_TRACKER = 14300;
  private static final int PORT_EXTERNAL_TRACKER = 14305;

  private NetworkManager networkManager1;
  private EvictingContractManager evictingContractManager1;
  private NetworkManager networkManager2;
  private EvictingContractManager evictingContractManager2;
  private NetworkManager networkManagerTracker;


  @BeforeEach
  void setUp() throws Exception {
    testFolder1 = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    testFolderPath1 = Paths.get(testFolder1);
    networkManager1 = new NetworkManagerImpl(testFolderPath1, PORT_INTERNAL_1, PORT_EXTERNAL_1);

    testFolder2 = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    testFolderPath2 = Paths.get(testFolder2);
    networkManager2 = new NetworkManagerImpl(testFolderPath2, PORT_INTERNAL_2, PORT_EXTERNAL_2);

    testFolder3 = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    testFolderPath3 = Paths.get(testFolder3);
    networkManagerTracker = new NetworkManagerImpl(testFolderPath3, PORT_INTERNAL_TRACKER, PORT_EXTERNAL_TRACKER);


    byte[] randBytes = new byte[20];
    RANDOM.nextBytes(randBytes);
    String peer1Rand = Base64.toBase64String(randBytes);
    RANDOM.nextBytes(randBytes);
    String peer2Rand = Base64.toBase64String(randBytes);
    evictingContractManager1 = new EvictingContractManager(3, peer1Rand, peer2Rand);
    evictingContractManager2 = new EvictingContractManager(3, peer2Rand, peer1Rand);
  }

  @Test
  void testConnection() throws Exception {
    // Inject PEX entry for Peer 2 into Peer 1
    // Add Peer2 as a contract to Peer1
    String peerID2 = networkManager2.getUID();
    networkManager1.setContractManager(evictingContractManager1);
    evictingContractManager1.addContractedPeer(peerID2);
    extractPexManager(networkManager1).addEntry(peerID2, "127.0.0.1", PORT_INTERNAL_2, false, DateTime.now(), new byte[0]);

    // Run Peer1
    networkManager1.run();
    // Should attempt to connect to Peer 2;
    boolean peer2Connected = false;
    for (int i = 0; i < 100 && !peer2Connected; i++) {
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
    String peerID1 = networkManager1.getUID();
    String peerID2 = networkManager2.getUID();
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
    for (int i = 0; i < 100 && !peer2Connected; i++) {
      Thread.sleep(50);
      peer2Connected = networkManager1.peerConnected(peerID2);
    }
    Assertions.assertTrue(peer2Connected);

    // Give them a second to exchange PEX info
    Thread.sleep(400);

    networkManager2.close();

    // Now reboot both hosts. If peer 2 is run it should auto reconnect...

    networkManager2 = new NetworkManagerImpl(testFolderPath2, PORT_INTERNAL_2, PORT_EXTERNAL_2);
    networkManager2.setContractManager(evictingContractManager2);

    networkManager2.run();

    boolean peer1Reconnected = false;
    for (int i = 0; i < 100 && !peer1Reconnected; i++) {
      Thread.sleep(50);
      peer1Reconnected = networkManager2.peerConnected(peerID1);
    }
    Assertions.assertTrue(peer1Reconnected);
  }

  @Test
  void sharingThroughTrackerConnection() throws Exception {
    // Inject new tracker into all of them.
    String trackerPeerId = networkManagerTracker.getUID();
    Tracker tracker = new Tracker(trackerPeerId, "127.0.0.1", PORT_INTERNAL_TRACKER);

    injectNewTracker(networkManager1, tracker, trackerPeerId);
    injectNewTracker(networkManager2, tracker, trackerPeerId);
    injectNewTracker(networkManagerTracker, tracker, trackerPeerId);

    // Skip the start time back in time 6 mins
    DateTime newStartTime = DateTime.now().minusMinutes(6);
    injectNewStartTime(networkManager1, newStartTime);
    injectNewStartTime(networkManager2, newStartTime);
    injectNewStartTime(networkManagerTracker, newStartTime);

    // Setup 1 & 2 as peers. 3 is a tracker.

    networkManager1.setContractManager(evictingContractManager1);
    networkManager2.setContractManager(evictingContractManager2);
    networkManager1.setSearchForNewPublicPeers(true);
    networkManager2.setSearchForNewPublicPeers(true);

    // Start all of them.

    networkManager1.run();
    networkManager2.run();
    networkManagerTracker.run();

    // Wait until the tracker is dialled

    String peerID1 = networkManager1.getUID();
    String peerID2 = networkManager2.getUID();

    boolean trackerConnected = false;
    for (int i = 0; i < 100 && !trackerConnected; i++) {
      Thread.sleep(100);
      trackerConnected = networkManagerTracker.peerConnected(peerID2) && networkManagerTracker.peerConnected(peerID1);
    }
    Assertions.assertTrue(trackerConnected);

    // Give the Peers a moment to send corrected (public) PEX info
    extractGatekeeper(networkManager1).maintainPeerPopulation();
    extractGatekeeper(networkManager2).maintainPeerPopulation();

    // Give a moment for the tracker to receive the PEX information.
    boolean pexInfoExchanged = false;
    PexManager pexManagerTracker = extractPexManager(networkManagerTracker);
    for (int i = 0; i < 200 && !pexInfoExchanged; i++) {
      Thread.sleep(50);
      // Remember that entries from the same IP overwrite each other
      pexInfoExchanged = !pexManagerTracker.getPublicEntries(10).isEmpty();
    }
    Assertions.assertTrue(pexInfoExchanged);

    // Give them a moment to exchange PEX info
    Thread.sleep(300);

    // Prompt Gatekeepers to run again. They should now have pulled each other PEX info from the tracker.
    extractGatekeeper(networkManager1).maintainPeerPopulation();
    extractGatekeeper(networkManager2).maintainPeerPopulation();

    // Check if they connect
    boolean peer1and2Connected = false;
    for (int i = 0; i < 200 && !peer1and2Connected; i++) {
      Thread.sleep(100);
      peer1and2Connected = networkManager1.peerConnected(peerID2) && networkManager2.peerConnected(peerID1);
    }
    Assertions.assertTrue(peer1and2Connected);

    // Give them a moment to exchange PEX info
    Thread.sleep(700);

    // Shutdown the tracker, should no longer be needed.
    networkManagerTracker.close();

    // Now reboot both hosts. They should auto-reconnect from PEX history.

    networkManager1.close();
    networkManager2.close();

    networkManager1 = new NetworkManagerImpl(testFolderPath1, PORT_INTERNAL_1, PORT_EXTERNAL_1);
    networkManager1.setContractManager(evictingContractManager1);
    networkManager2 = new NetworkManagerImpl(testFolderPath2, PORT_INTERNAL_2, PORT_EXTERNAL_2);
    networkManager2.setContractManager(evictingContractManager2);

    networkManager1.run();
    networkManager2.run();

    // Should have reconnected if PEX worked.

    peer1and2Connected = false;
    for (int i = 0; i < 100 && !peer1and2Connected; i++) {
      Thread.sleep(50);
      peer1and2Connected = networkManager1.peerConnected(peerID2) && networkManager2.peerConnected(peerID1);
    }
    Assertions.assertTrue(peer1and2Connected);

    Assertions.assertTrue(evictingContractManager1.getContractedPeers().contains(peerID2));
    Assertions.assertTrue(evictingContractManager2.getContractedPeers().contains(peerID1));
  }

  @Test
  void findingContractedPeerThroughTrackerConnection() throws Exception {
    // Inject new tracker into all of them.
    String trackerPeerId = networkManagerTracker.getUID();
    Tracker tracker = new Tracker(trackerPeerId, "127.0.0.1", PORT_INTERNAL_TRACKER);

    injectNewTracker(networkManager1, tracker, trackerPeerId);
    injectNewTracker(networkManager2, tracker, trackerPeerId);
    injectNewTracker(networkManagerTracker, tracker, trackerPeerId);

    // Skip the start time back in time 6 mins
    DateTime newStartTime = DateTime.now().minusMinutes(60);
    injectNewStartTime(networkManager1, newStartTime);
    injectNewStartTime(networkManager2, newStartTime);
    injectNewStartTime(networkManagerTracker, newStartTime);

    // Setup 1 & 2 as peers. 3 is a tracker.

    networkManager1.setContractManager(evictingContractManager1);
    networkManager2.setContractManager(evictingContractManager2);
    evictingContractManager1.addContractedPeer(networkManager2.getUID());
    evictingContractManager2.addContractedPeer(networkManager1.getUID());
    PeerCertManager peerCertManager1 = extractPeerCertManager(networkManager1);
    PeerCertManager peerCertManager2 = extractPeerCertManager(networkManager2);
    MembraneAuthInfo membraneAuthInfo1 = extractMembraneAuthInfo(networkManager1);
    MembraneAuthInfo membraneAuthInfo2 = extractMembraneAuthInfo(networkManager2);
    peerCertManager1.addCertificate(membraneAuthInfo2.getClientId(), membraneAuthInfo2.getX509Certificate());
    peerCertManager2.addCertificate(membraneAuthInfo1.getClientId(), membraneAuthInfo1.getX509Certificate());

    // Start all of them.

    networkManager1.run();
    networkManager2.run();
    networkManagerTracker.run();

    // Wait until the tracker is dialled

    String peerID1 = networkManager1.getUID();
    String peerID2 = networkManager2.getUID();

    boolean trackerConnected = false;
    for (int i = 0; i < 100 && !trackerConnected; i++) {
      Thread.sleep(100);
      trackerConnected = networkManagerTracker.peerConnected(peerID2) && networkManagerTracker.peerConnected(peerID1);
    }
    Assertions.assertTrue(trackerConnected);

    // Give the Peers a moment to send corrected (public) PEX info
    extractGatekeeper(networkManager1).maintainPeerPopulation();
    extractGatekeeper(networkManager2).maintainPeerPopulation();

    // Give a moment for the tracker to receive the PEX information.
    boolean pexInfoExchanged = false;
    PexManager pexManagerTracker = extractPexManager(networkManagerTracker);
    for (int i = 0; i < 200 && !pexInfoExchanged; i++) {
      Thread.sleep(50);
      // Remember that entries from the same IP overwrite each other
      pexInfoExchanged = !pexManagerTracker.getPexEntries().isEmpty();
    }
    Assertions.assertTrue(pexInfoExchanged);

    // Give them a moment to exchange PEX info
    Thread.sleep(300);

    // Prompt Gatekeepers to run again. They should now have pulled each other PEX info from the tracker.
    extractGatekeeper(networkManager1).maintainPeerPopulation();
    extractGatekeeper(networkManager2).maintainPeerPopulation();

    // Check if they connect
    boolean peer1and2Connected = false;
    for (int i = 0; i < 200 && !peer1and2Connected; i++) {
      Thread.sleep(100);
      peer1and2Connected = networkManager1.peerConnected(peerID2) && networkManager2.peerConnected(peerID1);
    }
    Assertions.assertTrue(peer1and2Connected);

    // Give them a moment to exchange PEX info
    Thread.sleep(700);

    // Shutdown the tracker, should no longer be needed.
    networkManagerTracker.close();

    // Now reboot both hosts. They should auto-reconnect from PEX history.

    networkManager1.close();
    networkManager2.close();

    networkManager1 = new NetworkManagerImpl(testFolderPath1, PORT_INTERNAL_1, PORT_EXTERNAL_1);
    networkManager1.setContractManager(evictingContractManager1);
    networkManager2 = new NetworkManagerImpl(testFolderPath2, PORT_INTERNAL_2, PORT_EXTERNAL_2);
    networkManager2.setContractManager(evictingContractManager2);

    networkManager1.run();
    networkManager2.run();

    // Should have reconnected if PEX worked.

    peer1and2Connected = false;
    for (int i = 0; i < 100 && !peer1and2Connected; i++) {
      Thread.sleep(50);
      peer1and2Connected = networkManager1.peerConnected(peerID2) && networkManager2.peerConnected(peerID1);
    }
    Assertions.assertTrue(peer1and2Connected);

    Assertions.assertTrue(evictingContractManager1.getContractedPeers().contains(peerID2));
    Assertions.assertTrue(evictingContractManager2.getContractedPeers().contains(peerID1));
  }

  @Test
  void testContractUpdateHandling() throws Exception {
    // Setup 1 & 2 as peers.

    networkManager1.setContractManager(evictingContractManager1);
    networkManager2.setContractManager(evictingContractManager2);

    evictingContractManager1.addContractedPeer(networkManager2.getUID());
    evictingContractManager2.addContractedPeer(networkManager1.getUID());
    PeerCertManager peerCertManager1 = extractPeerCertManager(networkManager1);
    PeerCertManager peerCertManager2 = extractPeerCertManager(networkManager2);
    MembraneAuthInfo membraneAuthInfo1 = extractMembraneAuthInfo(networkManager1);
    MembraneAuthInfo membraneAuthInfo2 = extractMembraneAuthInfo(networkManager2);
    peerCertManager1.addCertificate(membraneAuthInfo2.getClientId(), membraneAuthInfo2.getX509Certificate());
    peerCertManager2.addCertificate(membraneAuthInfo1.getClientId(), membraneAuthInfo1.getX509Certificate());

    PexManager pexManager2 = extractPexManager(networkManager2);
    pexManager2.addEntry(networkManager1.getUID(), "127.0.0.1", PORT_INTERNAL_1, false, DateTime.now(), new byte[0]);

    // Start all of them.

    networkManager1.run();
    networkManager2.run();

    // Check if they connect
    boolean peer1and2Connected = false;
    for (int i = 0; i < 200 && !peer1and2Connected; i++) {
      Thread.sleep(100);
      peer1and2Connected = networkManager1.peerConnected(networkManager2.getUID()) &&
          networkManager2.peerConnected(networkManager1.getUID());
    }
    Assertions.assertTrue(peer1and2Connected);

    // Send contract update

    Set<String> fullRemoteBlockList = ImmutableSet.of(
        evictingContractManager1.BLOCK_FULL_PEERS,
        evictingContractManager1.BLOCK_HASH_PEERS,
        evictingContractManager1.BLOCK_DELETE_PEERS);
    networkManager1.sendContractUpdateToPeer(networkManager2.getUID(), DateTime.now().hourOfDay().roundFloorCopy(), 1, fullRemoteBlockList);

    boolean blocksCorrectlyConfirmed = false;
    for (int i = 0; i < 200 && !blocksCorrectlyConfirmed; i++) {
      Thread.sleep(100);
      blocksCorrectlyConfirmed = evictingContractManager2.isHashConfirmed() &&
          evictingContractManager1.isBlockDeleted() &&
          evictingContractManager2.isFullBlockConfirmed();
    }
    Assertions.assertTrue(blocksCorrectlyConfirmed);

    Assertions.assertFalse(evictingContractManager1.isHashConfirmed() ||
        evictingContractManager2.isBlockDeleted() ||
        evictingContractManager1.isFullBlockConfirmed());
  }

  @Test
  void testSendingLargeDataBlock() throws Exception {
    // Setup 1 & 2 as peers.

    networkManager1.setContractManager(evictingContractManager1);
    networkManager2.setContractManager(evictingContractManager2);

    evictingContractManager1.addContractedPeer(networkManager2.getUID());
    evictingContractManager2.addContractedPeer(networkManager1.getUID());
    PeerCertManager peerCertManager1 = extractPeerCertManager(networkManager1);
    PeerCertManager peerCertManager2 = extractPeerCertManager(networkManager2);
    MembraneAuthInfo membraneAuthInfo1 = extractMembraneAuthInfo(networkManager1);
    MembraneAuthInfo membraneAuthInfo2 = extractMembraneAuthInfo(networkManager2);
    peerCertManager1.addCertificate(membraneAuthInfo2.getClientId(), membraneAuthInfo2.getX509Certificate());
    peerCertManager2.addCertificate(membraneAuthInfo1.getClientId(), membraneAuthInfo1.getX509Certificate());

    PexManager pexManager2 = extractPexManager(networkManager2);
    pexManager2.addEntry(networkManager1.getUID(), "127.0.0.1", PORT_INTERNAL_1, false, DateTime.now(), new byte[0]);

    // Start all of them.

    networkManager1.run();
    networkManager2.run();

    // Check if they connect
    boolean peer1and2Connected = false;
    for (int i = 0; i < 200 && !peer1and2Connected; i++) {
      Thread.sleep(100);
      peer1and2Connected = networkManager1.peerConnected(networkManager2.getUID()) &&
          networkManager2.peerConnected(networkManager1.getUID());
    }
    Assertions.assertTrue(peer1and2Connected);

    // Send contract update


    //byte[] largeBlockData = new byte[64*1024*1024]; // 64MB of random data.
    byte[] largeBlockData = new byte[64 * 1024 * 1024]; // 48MB of random data.
    RANDOM.nextBytes(largeBlockData);

    String blockId_1 = "blockId_1";
    networkManager1.uploadBlockToPeer(networkManager2.getUID(), blockId_1, largeBlockData);

    boolean blocksCorrectlyConfirmed = false;
    for (int i = 0; i < 200 && !blocksCorrectlyConfirmed; i++) {
      Thread.sleep(100);
      blocksCorrectlyConfirmed = evictingContractManager2.getReceivedBlockId() != null;
    }
    Assertions.assertTrue(blocksCorrectlyConfirmed);
    Assertions.assertEquals(blockId_1, evictingContractManager2.getReceivedBlockId());
    Assertions.assertArrayEquals(largeBlockData, evictingContractManager2.getReceivedBlock());
  }


  @AfterEach
  void tearDown() throws Exception {
    networkManager1.close();
    networkManager2.close();
    networkManagerTracker.close();
    StorageManagerTestUtils.deleteDirectoryRecursively(testFolderPath1);
    StorageManagerTestUtils.deleteDirectoryRecursively(testFolderPath2);
    StorageManagerTestUtils.deleteDirectoryRecursively(testFolderPath3);
  }

  private PexManager extractPexManager(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field pexField = networkManagerImpl.getClass().getDeclaredField("pexManager");
    pexField.setAccessible(true);
    return (PexManager) pexField.get(networkManagerImpl);
  }

  private Gatekeeper extractGatekeeper(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field pexField = networkManagerImpl.getClass().getDeclaredField("gatekeeper");
    pexField.setAccessible(true);
    return (Gatekeeper) pexField.get(networkManagerImpl);
  }

  private PeerCertManager extractPeerCertManager(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field pexField = networkManagerImpl.getClass().getDeclaredField("peerCertManager");
    pexField.setAccessible(true);
    return (PeerCertManager) pexField.get(networkManagerImpl);
  }

  private MembraneAuthInfo extractMembraneAuthInfo(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field pexField = networkManagerImpl.getClass().getDeclaredField("membraneAuthInfo");
    pexField.setAccessible(true);
    return (MembraneAuthInfo) pexField.get(networkManagerImpl);
  }

  private void injectNewStartTime(NetworkManager networkManager, DateTime newStartTime) throws Exception {
    Gatekeeper gatekeeper = extractGatekeeper(networkManager);
    Field startField = gatekeeper.getClass().getDeclaredField("startUpDateTime");
    startField.setAccessible(true);
    startField.set(gatekeeper, newStartTime);
  }

  private TrackerManager extractTrackerManager(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field trackerManager = networkManagerImpl.getClass().getDeclaredField("trackerManager");
    trackerManager.setAccessible(true);
    return (TrackerManager) trackerManager.get(networkManagerImpl);
  }

  private void injectNewTracker(NetworkManager networkManager, Tracker tracker, String uid) throws Exception {
    TrackerManager trackerManager = extractTrackerManager(networkManager);
    Field trackersField = trackerManager.getClass().getDeclaredField("trackers");
    trackersField.setAccessible(true);
    trackersField.set(trackerManager, Collections.singleton(tracker));
    Field trackerIdsField = trackerManager.getClass().getDeclaredField("trackerPeerIds");
    trackerIdsField.setAccessible(true);
    trackerIdsField.set(trackerManager, Collections.singleton(uid));
  }
}