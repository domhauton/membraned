package com.domhauton.membrane.network.gatekeeper;

import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messages.PexAdvertisement;
import com.domhauton.membrane.network.messages.PexQueryRequest;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.upnp.ExternalAddress;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 09/04/17.
 */
abstract class PexUtils {
  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * Send updated PEX information to all peers.
   *
   * @param isPublic should the PEX information be re-distributed
   */
  static void sendPexUpdate(PortForwardingService portForwardingService, Collection<Peer> connectedPeers, boolean isPublic) {
    LOGGER.info("Sending {} PEX updates to {} peers.", isPublic ? "Public" : "Non-Public", connectedPeers.size());
    Iterator<ExternalAddress> externalAddresses = portForwardingService.getExternallyMappedAddresses().iterator();
    ExternalAddress externalAddress = externalAddresses.hasNext() ?
        externalAddresses.next() : portForwardingService.getNonForwardedAddress();
    PexAdvertisement pexAdvertisement = new PexAdvertisement(externalAddress.getIpAddress(), externalAddress.getPort(), isPublic, DateTime.now());

    for (Peer peer : connectedPeers) {
      try {
        peer.sendPeerMessage(pexAdvertisement);
      } catch (PeerException e) {
        LOGGER.warn("Failed to send PEX request to [{}]. {}", peer.getUid(), e.getMessage());
      }
    }
  }

  /**
   * Attempt connections to provided public PEX entries.
   */
  static void connectToPublicPeersInPex(PexManager pexManager, ConnectionManager connectionManager, int requiredConnections) {
    // Shouldn't be less than zero. Clamping.
    requiredConnections = Math.max(0, requiredConnections);
    LOGGER.info("Connecting to {} peers in public pex.", requiredConnections);
    pexManager.getPublicEntries(requiredConnections)
        .forEach(x -> connectionManager.connectToPeer(x.getAddress(), x.getPort()));
  }

  /**
   * Requests all connected peers for PEX information
   */
  static void requestPexInformation(ConnectionManager connectionManager, Set<String> contractedPeers, boolean searchingForNewPeers) {
    LOGGER.info("Requesting peers for PEX update.");
    Set<String> lostPeers = new HashSet<>();
    lostPeers.addAll(contractedPeers);
    lostPeers.removeAll(connectionManager.getAllConnectedPeers().stream().map(Peer::getUid).collect(Collectors.toSet()));

    if (!lostPeers.isEmpty()) {
      PexQueryRequest pexQueryRequest = new PexQueryRequest(contractedPeers, searchingForNewPeers);
      for (Peer peer : connectionManager.getAllConnectedPeers()) {
        try {
          peer.sendPeerMessage(pexQueryRequest);
        } catch (PeerException e) {
          LOGGER.warn("Failed to send PEX request to [{}]. {}", peer.getUid(), e.getMessage());
        }
      }
    }
  }
}
