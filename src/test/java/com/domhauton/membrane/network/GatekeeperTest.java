package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.pex.PexEntry;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.tracker.TrackerManager;
import com.domhauton.membrane.network.upnp.ExternalAddress;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dominic on 31/03/17.
 */
class GatekeeperTest {

  private ConnectionManager connectionManager;
  private ContractManager contractManager;
  private PexManager pexManager;
  private PortForwardingService portForwardingService;
  private PeerCertManager peerCertManager;
  private Gatekeeper gatekeeper;
  private TrackerManager trackerManager;

  private Peer peerMock1;
  private Peer peerMock2;

  private Set<Peer> connectedPeers = new HashSet<>();
  private Set<String> contractedPeers = new HashSet<>();
  private ContractManager contractManagerMock;

  private final static String PEER_ID_1 = "peer_1";
  private final static String PEER_IP_1 = "192.168.1.2";
  private final static int PEER_PORT_1 = 81;
  private PexEntry pexEntry1;

  private final static String PEER_ID_2 = "peer_2";
  private final static String PEER_IP_2 = "192.168.1.3";
  private final static int PEER_PORT_2 = 82;
  private PexEntry pexEntry2;

  private final static String EXTERNAL_ADDR = "192.168.0.3";
  private final static int EXTERNAL_PORT = 12345;

  @BeforeEach
  void setUp() throws Exception {
    connectionManager = Mockito.mock(ConnectionManager.class);

    Mockito.doAnswer(invocation -> connectedPeers.stream().map(Peer::getUid).collect(Collectors.toSet()))
        .when(connectionManager)
        .getAllConnectedPeerIds();
    Mockito.when(connectionManager.getAllConnectedPeers()).thenReturn(connectedPeers);

    pexManager = Mockito.mock(PexManager.class);

    // Fully mock the port forwarding service

    ExternalAddress externalAddress = Mockito.mock(ExternalAddress.class);
    Mockito.when(externalAddress.getPort()).thenReturn(EXTERNAL_PORT);
    Mockito.when(externalAddress.getIpAddress()).thenReturn(EXTERNAL_ADDR);

    portForwardingService = Mockito.mock(PortForwardingService.class);
    Mockito.when(portForwardingService.getExternalAddress()).thenReturn(externalAddress);

    peerCertManager = Mockito.mock(PeerCertManager.class);
    trackerManager = new TrackerManager();
    gatekeeper = new Gatekeeper(connectionManager, pexManager, portForwardingService, peerCertManager, trackerManager, 5);

    peerMock1 = Mockito.mock(Peer.class);
    peerMock2 = Mockito.mock(Peer.class);
    Mockito.when(peerMock1.getUid()).thenReturn(PEER_ID_1);
    Mockito.when(peerMock2.getUid()).thenReturn(PEER_ID_2);

    contractManagerMock = Mockito.mock(ContractManager.class);
    Mockito.when(contractManagerMock.getContractCountTarget()).thenReturn(3);
    Mockito.when(contractManagerMock.getContractedPeers()).thenReturn(contractedPeers);

    pexEntry1 = new PexEntry(PEER_IP_1, PEER_PORT_1, false, DateTime.now(), new byte[0]);
    pexEntry2 = new PexEntry(PEER_IP_2, PEER_PORT_2, false, DateTime.now(), new byte[0]);
  }

  @Test
  void newUncontractedPeerJoinTest() {
    gatekeeper.processNewPeerConnected(peerMock1);
  }

  @Test
  void setNewPeerNoPeersWantedTest() throws Exception {
    connectedPeers.add(peerMock1);
    gatekeeper.processNewPeerConnected(peerMock1);
    Mockito.when(contractManagerMock.getContractCountTarget()).thenReturn(0);
    gatekeeper.setContractManager(contractManagerMock);
    Mockito.verify(contractManagerMock, Mockito.never()).addContractedPeer(Mockito.anyString());

    gatekeeper.setSearchForNewPublicPeers(true);
    Mockito.verify(contractManagerMock, Mockito.never()).addContractedPeer(Mockito.anyString());
  }

  @Test
  void setNewPeerPeersWantedTest() throws Exception {
    connectedPeers.add(peerMock1);
    gatekeeper.processNewPeerConnected(peerMock1);
    gatekeeper.setContractManager(contractManagerMock);
    Mockito.verify(contractManagerMock, Mockito.never()).addContractedPeer(Mockito.anyString());

    // Only add peer after search for new peers enabled

    gatekeeper.setSearchForNewPublicPeers(true);
    Mockito.verify(contractManagerMock, Mockito.times(1)).addContractedPeer(PEER_ID_1);
  }

  @Test
  void contractedPeerJoinedNoPeersWantedTest() throws Exception {
    connectedPeers.add(peerMock1);
    gatekeeper.processNewPeerConnected(peerMock1);
    contractedPeers.add(peerMock1.getUid());
    Mockito.when(contractManagerMock.getContractCountTarget()).thenReturn(0);
    gatekeeper.setContractManager(contractManagerMock);
    Mockito.verify(contractManagerMock, Mockito.times(1)).addContractedPeer(Mockito.anyString());

    gatekeeper.setSearchForNewPublicPeers(true);
    Mockito.verify(contractManagerMock, Mockito.times(2)).addContractedPeer(Mockito.anyString());
  }

  @Test
  void newPexInformation() throws Exception {
    gatekeeper.processNewPexEntry(peerMock1.getUid(), pexEntry1);
    Mockito.verify(connectionManager, Mockito.never()).connectToPeer(Mockito.anyString(), Mockito.anyInt());
    connectedPeers.add(peerMock1);
    gatekeeper.processNewPexEntry(peerMock1.getUid(), pexEntry1);
    Mockito.verify(connectionManager, Mockito.never()).connectToPeer(Mockito.anyString(), Mockito.anyInt());
  }

  @Test
  void connectedPeerPexInformation() throws Exception {
    contractedPeers.add(peerMock1.getUid());
    gatekeeper.setContractManager(contractManagerMock);

    gatekeeper.processNewPexEntry(peerMock1.getUid(), pexEntry1);
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_1, PEER_PORT_1);

    connectedPeers.add(peerMock1);
    gatekeeper.processNewPexEntry(peerMock1.getUid(), pexEntry1);
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(Mockito.anyString(), Mockito.anyInt());
  }

  @Test
  void newPeerPexInformation() throws Exception {
    gatekeeper.setContractManager(contractManagerMock);

    gatekeeper.processNewPexEntry(peerMock1.getUid(), pexEntry1);
    Mockito.verify(connectionManager, Mockito.never()).connectToPeer(Mockito.anyString(), Mockito.anyInt());

    gatekeeper.setSearchForNewPublicPeers(true);

    gatekeeper.processNewPexEntry(peerMock1.getUid(), pexEntry1);
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(Mockito.anyString(), Mockito.anyInt());
  }
}