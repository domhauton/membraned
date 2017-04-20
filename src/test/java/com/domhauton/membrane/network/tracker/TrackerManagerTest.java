package com.domhauton.membrane.network.tracker;

import com.domhauton.membrane.network.connection.ConnectionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Created by dominic on 10/04/17.
 */
class TrackerManagerTest {

  private TrackerManager trackerManager;

  @BeforeEach
  void setUp() {
    trackerManager = new TrackerManager();
  }

  @Test
  void checkTrackersExist() {
    Assertions.assertFalse(trackerManager.getTrackerIds().isEmpty());
  }

  @Test
  void checkTrackerConnect() {
    ConnectionManager connectionManager = Mockito.mock(ConnectionManager.class);
    trackerManager.connectToTrackers(connectionManager);
    Mockito.verify(connectionManager, Mockito.atLeastOnce()).connectToPeer(Mockito.anyString(), Mockito.anyInt());
  }

  @Test
  void checkTrackers() {
    // No peers needed
    Assertions.assertFalse(trackerManager.shouldConnectToTrackers(-1, 20, 0));
    Assertions.assertFalse(trackerManager.shouldConnectToTrackers(0, 20, 0));
    Assertions.assertFalse(trackerManager.shouldConnectToTrackers(0, 20, 10));
    // Below startup time
    Assertions.assertTrue(trackerManager.shouldConnectToTrackers(10, 20, 0));
    Assertions.assertFalse(trackerManager.shouldConnectToTrackers(10, 4, 0));

    // Below curve
    Assertions.assertTrue(trackerManager.shouldConnectToTrackers(20, 5, 0));
    Assertions.assertTrue(trackerManager.shouldConnectToTrackers(20, 10, 0));
    Assertions.assertFalse(trackerManager.shouldConnectToTrackers(20, 10, 1));

    Assertions.assertTrue(trackerManager.shouldConnectToTrackers(20, 11, 1));
    Assertions.assertTrue(trackerManager.shouldConnectToTrackers(20, 20, 1));
    Assertions.assertFalse(trackerManager.shouldConnectToTrackers(20, 20, 2));

    Assertions.assertTrue(trackerManager.shouldConnectToTrackers(20, 200, 19));
    Assertions.assertFalse(trackerManager.shouldConnectToTrackers(20, 200, 20));
  }
}