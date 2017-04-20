package com.domhauton.membrane.network;

import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.tracker.Tracker;
import com.domhauton.membrane.network.tracker.TrackerManager;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Created by dominic on 11/04/17.
 */
class NetworkManagerImplTest {

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

    evictingContractManager1 = new EvictingContractManager(3);
    evictingContractManager2 = new EvictingContractManager(3);
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
      Thread.sleep(50);
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