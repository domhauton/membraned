package com.domhauton.membrane.network.tracker;

import com.domhauton.membrane.network.connection.ConnectionManager;
import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 09/04/17.
 */
public class TrackerManager {
  private final static int WAIT_FROM_STARTUP = 5; // Do not connect if it has been less than this time.
  private final static int EXPECTED_CONNECT_TIME = 200; // Connect to trackers if peers have not been saturated in this time.

  private final static Set<Tracker> DEFAULT_ENTRIES = ImmutableSet.of(
      new Tracker("", "caelum.domhauton.com", 14200)
  );

  private final Logger logger = LogManager.getLogger();

  private final Set<Tracker> trackers;
  private final Set<String> trackerPeerIds;

  public TrackerManager() {
    this(DEFAULT_ENTRIES);
  }

  private TrackerManager(Set<Tracker> trackers) {
    this.trackers = trackers;
    trackerPeerIds = trackers.stream()
        .map(Tracker::getPeerId)
        .collect(Collectors.toSet());
  }

  public Set<String> getTrackerIds() {
    return trackerPeerIds;
  }

  /**
   * Should a tracker connection be initiated based on startup time, and expected peer ratios.
   *
   * @param contractedPeerTarget Number of expected contracted peers
   * @param minutesFromStartup   Minutes since membrane startup
   * @param connectedPeerCount   Number of peers currently connected
   * @return True if connection to trackers is needed
   */
  public boolean shouldConnectToTrackers(final int contractedPeerTarget, final int minutesFromStartup, final int connectedPeerCount) {
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
  public void connectToTrackers(ConnectionManager connectionManager) {
    logger.info("Initiating connection to {} tracker/s.", trackers.size());
    trackers.forEach(x -> connectionManager.connectToPeer(x.getIp(), x.getPort()));
  }
}
