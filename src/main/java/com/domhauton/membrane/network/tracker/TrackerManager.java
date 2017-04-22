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
      new Tracker("672da60b8d67742e35ab9384d0b8983fe275a8f9da70636739d2f24251cf9025", "caelum.domhauton.com", 14200)
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
  public boolean shouldConnectToTrackers(final int contractedPeerTarget, boolean hasPexEntries, final int minutesFromStartup, final int connectedPeerCount) {

    if (contractedPeerTarget < 1) {
      logger.debug("Contracted peers not required. Not connecting to tracker.");
      return false;
    } else if (!hasPexEntries) {
      logger.debug("No pex entries and peers required. Connecting to tracker.");
      return true;
    } else if (minutesFromStartup < WAIT_FROM_STARTUP) {
      logger.debug("To early before launch to connect to tracker. Wait at least {} mins.", WAIT_FROM_STARTUP);
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
    for (Tracker tracker : trackers) {
      logger.info("Dialling tracker {}:{}. [{}]", tracker.getIp(), tracker.getPort(), tracker.getPeerId());
      connectionManager.connectToPeer(tracker.getIp(), tracker.getPort());
    }
    logger.info("Complete initiating connections to {} tracker/s.", trackers.size());
  }
}
