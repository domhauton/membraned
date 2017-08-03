package com.domhauton.membrane.network;

import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.storage.StorageManagerTestUtils;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by dominic on 11/04/17.
 */
@Disabled
class TrackerAvailabilityTest {
  private final static Random RANDOM = new SecureRandom();

  private String testFolder1;
  private Path testFolderPath1;

  private static final int PORT_INTERNAL_1 = 14133;
  private static final int PORT_EXTERNAL_1 = 14133;

  private NetworkManager networkManager1;
  private EvictingContractManager evictingContractManager1;


  @BeforeEach
  void setUp() throws Exception {
    testFolder1 = StorageManagerTestUtils.createRandomFolder(StorageManagerTestUtils.BASE_DIR);
    testFolderPath1 = Paths.get(testFolder1);
    networkManager1 = new NetworkManagerImpl(testFolderPath1, PORT_INTERNAL_1, PORT_EXTERNAL_1);


    byte[] randBytes = new byte[20];
    RANDOM.nextBytes(randBytes);
    String peer1Rand = Base64.toBase64String(randBytes);
    RANDOM.nextBytes(randBytes);
    String peer2Rand = Base64.toBase64String(randBytes);
    evictingContractManager1 = new EvictingContractManager(3, peer1Rand, peer2Rand);
  }

  @Test
  void testTrackerAvailability() throws Exception {
    // Setup Peer.
    networkManager1.setContractManager(evictingContractManager1);
    networkManager1.run();

    // Check if they connect to the tracker
    boolean trackerConnected = false;
    ConnectionManager connectionManager1 = extractConnectionManager(networkManager1);
    for (int i = 0; i < 200 && !trackerConnected; i++) {
      Thread.sleep(100);
      trackerConnected = connectionManager1.getAllConnectedPeerIds().size() > 0;
    }
    Assertions.assertTrue(trackerConnected);
  }

  @AfterEach
  void tearDown() throws Exception {
    networkManager1.close();
    StorageManagerTestUtils.deleteDirectoryRecursively(testFolderPath1);
  }

  private ConnectionManager extractConnectionManager(NetworkManager networkManager) throws Exception {
    NetworkManagerImpl networkManagerImpl = (NetworkManagerImpl) networkManager;
    Field pexField = networkManagerImpl.getClass().getDeclaredField("connectionManager");
    pexField.setAccessible(true);
    return (ConnectionManager) pexField.get(networkManagerImpl);
  }
}