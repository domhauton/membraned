package com.domhauton.membrane.network.gatekeeper;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by dominic on 10/04/17.
 */
abstract class GatekeeperUtils {

  /**
   * Total number of connections required to reach target for contract. Accounting for spare connections for contracted
   * and unconnected peers
   *
   * @return number of required connections. Can be negative.
   */
  static int requiredPeers(int knownPeerCount, int contractManagerTarget) {
    return contractManagerTarget - knownPeerCount;
  }

  /**
   * Total number of connections remaining.
   *
   * @return number of required connections. Can be negative.
   */
  static int remainingConnections(int knownPeerCount, int maxConnections) {
    return knownPeerCount - maxConnections;
  }

  /**
   * Find number of contracted peers union the number of connected peers
   *
   * @param contractedPeers Set of all contracted peers
   * @param connectedPeers  Set of all connected peers
   * @param friendPeers     Set of all friend peers
   * @param trackerIds      Set of all tracker peers
   * @return Number of peers contracted union those connected ignoring trackers.
   */
  static int getKnownPeerCount(Set<String> contractedPeers, Set<String> connectedPeers, Set<String> friendPeers, Set<String> trackerIds) {
    Set<String> knownPeers = new HashSet<>();
    knownPeers.addAll(contractedPeers);
    knownPeers.addAll(connectedPeers);
    knownPeers.addAll(friendPeers);
    knownPeers.removeAll(trackerIds);
    return knownPeers.size();
  }
}
