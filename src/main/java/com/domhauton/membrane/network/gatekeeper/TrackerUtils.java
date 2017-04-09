package com.domhauton.membrane.network.gatekeeper;

import com.domhauton.membrane.network.connection.ConnectionManager;
import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 09/04/17.
 */
abstract class TrackerUtils {
  private final static int WAIT_FROM_STARTUP = 5; // Do not connect if it has been less than this time.
  private final static int EXPECTED_CONNECT_TIME = 200; // Connect to trackers if peers have not been saturated in this time.
  private final static Set<Tracker> TRACKERS = ImmutableSet.of(
      new Tracker("", "192.168.0.1", 3000),
      new Tracker("", "192.168.0.2", 3000)
  );
  private final static Set<String> TRACKER_PEER_IDS = TRACKERS.stream().map(Tracker::getPeerId)
      .collect(Collectors.toSet());


  static Set<String> getTrackerIds() {
    return TRACKER_PEER_IDS;
  }

  /**
   * Should a tracker connection be initiated based on startup time, and expected peer ratios.
   *
   * @param contractedPeerTarget Number of expected contracted peers
   * @param minutesFromStartup   Minutes since membrane startup
   * @param connectedPeerCount   Number of peers currently connected
   * @return True if connection to trackers is needed
   */
  static boolean shouldConnectToTrackers(final int contractedPeerTarget, final int minutesFromStartup, final int connectedPeerCount) {
    if (contractedPeerTarget < 1 || minutesFromStartup < WAIT_FROM_STARTUP) {
      return false;
    }

    float connectedPeerRatio = (float) connectedPeerCount / (float) contractedPeerTarget;
    float expectedPeerRatio = (float) minutesFromStartup / (float) EXPECTED_CONNECT_TIME;
    return connectedPeerRatio < expectedPeerRatio;
  }

  /**
   * Initiates a connection to every tracker on the tracker list.
   */
  static void connectToTrackers(ConnectionManager connectionManager) {
    TRACKERS.forEach(x -> connectionManager.connectToPeer(x.getIp(), x.getPort()));
  }
}
