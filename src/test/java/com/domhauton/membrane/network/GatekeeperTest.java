package com.domhauton.membrane.network;

import com.domhauton.membrane.distributed.ContractManager;
import com.domhauton.membrane.network.auth.PeerCertManager;
import com.domhauton.membrane.network.connection.ConnectionManager;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.messages.PeerMessage;
import com.domhauton.membrane.network.messages.PexAdvertisement;
import com.domhauton.membrane.network.messages.PexQueryRequest;
import com.domhauton.membrane.network.pex.PexEntry;
import com.domhauton.membrane.network.pex.PexException;
import com.domhauton.membrane.network.pex.PexManager;
import com.domhauton.membrane.network.tracker.TrackerManager;
import com.domhauton.membrane.network.upnp.ExternalAddress;
import com.domhauton.membrane.network.upnp.PortForwardingService;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
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
  private ExternalAddress externalAddress;


  private Set<Peer> connectedPeers = new HashSet<>();
  private Set<String> contractedPeers = new HashSet<>();
  private ContractManager contractManagerMock;

  private final static String PEER_ID_1 = "peer_1";
  private final static String PEER_IP_1 = "192.168.1.2";
  private final static int PEER_PORT_1 = 81;
  private Peer peerMock1;
  private PexEntry pexEntry1;

  private final static String PEER_ID_2 = "peer_2";
  private final static String PEER_IP_2 = "192.168.1.3";
  private final static int PEER_PORT_2 = 82;
  private Peer peerMock2;
  private PexEntry pexEntry2;

  private final static String PEER_ID_3 = "peer_3";
  private Peer peerMock3;

  private final static String PEER_ID_4 = "peer_4";
  private Peer peerMock4;

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

    externalAddress = Mockito.mock(ExternalAddress.class);
    Mockito.when(externalAddress.getPort()).thenReturn(EXTERNAL_PORT);
    Mockito.when(externalAddress.getIpAddress()).thenReturn(EXTERNAL_ADDR);

    portForwardingService = Mockito.mock(PortForwardingService.class);
    Mockito.when(portForwardingService.getExternalAddress()).thenReturn(externalAddress);

    peerCertManager = Mockito.mock(PeerCertManager.class);
    trackerManager = new TrackerManager();
    gatekeeper = new Gatekeeper(connectionManager, pexManager, portForwardingService, peerCertManager, trackerManager, 3);

    peerMock1 = Mockito.mock(Peer.class);
    peerMock2 = Mockito.mock(Peer.class);
    peerMock3 = Mockito.mock(Peer.class);
    peerMock4 = Mockito.mock(Peer.class);
    Mockito.when(peerMock1.getUid()).thenReturn(PEER_ID_1);
    Mockito.when(peerMock2.getUid()).thenReturn(PEER_ID_2);
    Mockito.when(peerMock3.getUid()).thenReturn(PEER_ID_3);
    Mockito.when(peerMock4.getUid()).thenReturn(PEER_ID_4);

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

  @Test
  void peerMaintenanceUseSinglePexEntry() throws Exception {
    gatekeeper.setContractManager(contractManagerMock);
    gatekeeper.setSearchForNewPublicPeers(true);
    Mockito.when(pexManager.getEntry(peerMock1.getUid())).thenReturn(pexEntry1);
    Mockito.when(pexManager.getEntry(peerMock2.getUid())).thenThrow(new PexException("Mockito Exception"));

    gatekeeper.maintainPeerPopulation();
    // Will try to connect to tracker
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(Mockito.anyString(), Mockito.anyInt());

    contractedPeers.add(peerMock1.getUid());
    contractedPeers.add(peerMock2.getUid());
    gatekeeper.maintainPeerPopulation();
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_1, PEER_PORT_1);
    Mockito.verify(connectionManager, Mockito.times(3)).connectToPeer(Mockito.anyString(), Mockito.anyInt());
    connectedPeers.add(peerMock1);

    gatekeeper.maintainPeerPopulation();
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_1, PEER_PORT_1);
    Mockito.verify(connectionManager, Mockito.times(4)).connectToPeer(Mockito.anyString(), Mockito.anyInt());
  }

  @Test
  void peerMaintenanceIgnorePEXEntries() throws Exception {
    gatekeeper.setContractManager(contractManagerMock);
    gatekeeper.setSearchForNewPublicPeers(true);
    Mockito.when(pexManager.getEntry(peerMock1.getUid())).thenReturn(pexEntry1);
    Mockito.when(pexManager.getEntry(peerMock2.getUid())).thenReturn(pexEntry2);

    gatekeeper.maintainPeerPopulation();
    // Will try to connect to tracker
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(Mockito.anyString(), Mockito.anyInt());

    contractedPeers.add(peerMock1.getUid());
    contractedPeers.add(peerMock2.getUid());
    gatekeeper.maintainPeerPopulation();
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_1, PEER_PORT_1);
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_2, PEER_PORT_2);
    Mockito.verify(connectionManager, Mockito.times(4)).connectToPeer(Mockito.anyString(), Mockito.anyInt());
    connectedPeers.add(peerMock1);
    connectedPeers.add(peerMock2);

    gatekeeper.maintainPeerPopulation();
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_1, PEER_PORT_1);
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_2, PEER_PORT_2);
    Mockito.verify(connectionManager, Mockito.times(5)).connectToPeer(Mockito.anyString(), Mockito.anyInt());
  }

  @Test
  void peerMaintenanceCheckIfPEXCalledNonPublic() throws Exception {
    gatekeeper.setContractManager(contractManagerMock);
    Mockito.when(pexManager.getEntry(peerMock1.getUid())).thenReturn(pexEntry1);
    Mockito.when(pexManager.getEntry(peerMock2.getUid())).thenThrow(new PexException("Mockito Exception"));

    gatekeeper.maintainPeerPopulation();
    // Will try to connect to tracker
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(Mockito.anyString(), Mockito.anyInt());

    contractedPeers.add(peerMock1.getUid());
    contractedPeers.add(peerMock2.getUid());
    gatekeeper.maintainPeerPopulation();
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_1, PEER_PORT_1);
    Mockito.verify(connectionManager, Mockito.times(3)).connectToPeer(Mockito.anyString(), Mockito.anyInt());
    connectedPeers.add(peerMock1);

    // Check peer messages sent

    gatekeeper.maintainPeerPopulation();

    ArgumentCaptor<PeerMessage> peerMessageCaptor = ArgumentCaptor.forClass(PeerMessage.class);
    Mockito.verify(peerMock1, Mockito.times(2)).sendPeerMessage(peerMessageCaptor.capture());

    for (PeerMessage peerMessage : peerMessageCaptor.getAllValues()) {
      if (peerMessage.getClass().isInstance(PexAdvertisement.class)) {
        PexAdvertisement pexAdvertisement = (PexAdvertisement) peerMessage;
        Assertions.assertEquals(EXTERNAL_ADDR, pexAdvertisement.getIp());
        Assertions.assertEquals(EXTERNAL_PORT, pexAdvertisement.getPort());
      } else if (peerMessage.getClass().isInstance(PexQueryRequest.class)) {
        PexQueryRequest pexQueryRequest = (PexQueryRequest) peerMessage;
        Assertions.assertEquals(Collections.singleton(peerMock1.getUid()), pexQueryRequest.getRequestedPeers());
        Assertions.assertEquals(false, pexQueryRequest.isRequestPublic());
      }
    }
  }

  @Test
  void peerMaintenanceCheckIfPEXCalledPublic() throws Exception {
    gatekeeper.setContractManager(contractManagerMock);
    gatekeeper.setSearchForNewPublicPeers(true);
    Mockito.when(pexManager.getEntry(peerMock1.getUid())).thenReturn(pexEntry1);
    Mockito.when(pexManager.getEntry(peerMock2.getUid())).thenThrow(new PexException("Mockito Exception"));

    gatekeeper.maintainPeerPopulation();
    // Will try to connect to tracker
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(Mockito.anyString(), Mockito.anyInt());

    contractedPeers.add(peerMock1.getUid());
    contractedPeers.add(peerMock2.getUid());
    gatekeeper.maintainPeerPopulation();
    Mockito.verify(connectionManager, Mockito.times(1)).connectToPeer(PEER_IP_1, PEER_PORT_1);
    Mockito.verify(connectionManager, Mockito.times(3)).connectToPeer(Mockito.anyString(), Mockito.anyInt());
    connectedPeers.add(peerMock1);

    // Check peer messages sent

    gatekeeper.maintainPeerPopulation();

    ArgumentCaptor<PeerMessage> peerMessageCaptor = ArgumentCaptor.forClass(PeerMessage.class);
    Mockito.verify(peerMock1, Mockito.times(2)).sendPeerMessage(peerMessageCaptor.capture());

    for (PeerMessage peerMessage : peerMessageCaptor.getAllValues()) {
      if (peerMessage.getClass().isInstance(PexAdvertisement.class)) {
        PexAdvertisement pexAdvertisement = (PexAdvertisement) peerMessage;
        Assertions.assertEquals(EXTERNAL_ADDR, pexAdvertisement.getIp());
        Assertions.assertEquals(EXTERNAL_PORT, pexAdvertisement.getPort());
      } else if (peerMessage.getClass().isInstance(PexQueryRequest.class)) {
        PexQueryRequest pexQueryRequest = (PexQueryRequest) peerMessage;
        Assertions.assertEquals(Collections.singleton(peerMock1.getUid()), pexQueryRequest.getRequestedPeers());
        Assertions.assertEquals(true, pexQueryRequest.isRequestPublic());
      }
    }
  }

  @Test
  void checkPeersNoDisconnectsTest() throws Exception {
    gatekeeper.setContractManager(contractManagerMock);

    connectedPeers.addAll(ImmutableSet.of(peerMock1, peerMock2, peerMock3, peerMock4));
    contractedPeers.addAll(ImmutableSet.of(peerMock1.getUid(), peerMock2.getUid(), peerMock3.getUid(), peerMock4.getUid()));

    gatekeeper.maintainPeerPopulation();

    Mockito.verify(peerMock3, Mockito.never()).close();
    Mockito.verify(peerMock1, Mockito.never()).close();
    Mockito.verify(peerMock2, Mockito.never()).close();
    Mockito.verify(peerMock4, Mockito.never()).close();
  }

  @Test
  void selectCorrectPeerForDisconnectTest() throws Exception {
    gatekeeper.setContractManager(contractManagerMock);

    connectedPeers.addAll(ImmutableSet.of(peerMock1, peerMock2, peerMock3, peerMock4));
    contractedPeers.addAll(ImmutableSet.of(peerMock1.getUid(), peerMock2.getUid(), peerMock4.getUid()));

    gatekeeper.maintainPeerPopulation();

    Mockito.verify(peerMock1, Mockito.never()).close();
    Mockito.verify(peerMock2, Mockito.never()).close();
    Mockito.verify(peerMock4, Mockito.never()).close();
    Mockito.verify(peerMock3, Mockito.times(1)).close();
  }

  @Test
  void checkTrackersConnectedTest() throws Exception {
    Field field = gatekeeper.getClass().getDeclaredField("startUpDateTime");
    field.setAccessible(true);
    field.set(gatekeeper, DateTime.now().minusMinutes(6));

    gatekeeper.setContractManager(contractManagerMock);
    contractedPeers.addAll(ImmutableSet.of(peerMock1.getUid(), peerMock2.getUid(), peerMock4.getUid()));

    Mockito.when(pexManager.getEntry(peerMock1.getUid())).thenThrow(new PexException("Mockito Exception"));
    Mockito.when(pexManager.getEntry(peerMock2.getUid())).thenThrow(new PexException("Mockito Exception"));
    Mockito.when(pexManager.getEntry(peerMock4.getUid())).thenThrow(new PexException("Mockito Exception"));

    gatekeeper.maintainPeerPopulation();

    Mockito.verify(connectionManager, Mockito.times(1))
        .connectToPeer(Mockito.anyString(), Mockito.anyInt());
  }
}