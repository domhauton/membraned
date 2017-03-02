package com.domhauton.membrane.network.connection;

import com.domhauton.membrane.MembraneBuild;
import com.domhauton.membrane.network.auth.AuthUtils;
import com.domhauton.membrane.network.auth.MembraneAuthInfo;
import com.domhauton.membrane.network.connection.peer.Peer;
import com.domhauton.membrane.network.connection.peer.PeerException;
import com.domhauton.membrane.network.messaging.messages.PeerMessage;
import com.domhauton.membrane.network.messaging.messages.PeerMessageActions;
import com.domhauton.membrane.network.messaging.messages.PingMessage;
import com.domhauton.membrane.network.messaging.messages.PongMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Created by dominic on 13/02/17.
 */
class ConnectionManagerTest {
  private static final String TEST_SENDER = "user1";
  private static final String TEST_RECIPIENT = "user2";


  private MembraneAuthInfo membraneAuthInfo1;
  private MembraneAuthInfo membraneAuthInfo2;
  private ConnectionManager connectionManager1;
  private ConnectionManager connectionManager2;

  @BeforeEach
  void setUp() throws Exception {
    // System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
    // System.setProperty("javax.net.debug", "ssl");
    AuthUtils.addProvider();
    membraneAuthInfo1 = AuthUtils.generateAuthenticationInfo();
    membraneAuthInfo2 = AuthUtils.generateAuthenticationInfo();
    connectionManager1 = new ConnectionManager(membraneAuthInfo1, 12450);
    connectionManager2 = new ConnectionManager(membraneAuthInfo2, 12451);
  }

  @Test
  void establishConnection() throws Exception {
    CompletableFuture<Boolean> con1Callback = new CompletableFuture<>();
    CompletableFuture<Boolean> con2Callback = new CompletableFuture<>();

    connectionManager1.registerNewPeerCallback(peer -> con1Callback.complete(true));
    connectionManager2.registerNewPeerCallback(peer -> con2Callback.complete(true));

    connectionManager2.connectToPeer("127.0.0.1", 12450);

    Assertions.assertTrue(con1Callback.get(5, TimeUnit.SECONDS),
            "Connection Established in Con Manager 1");

    Assertions.assertTrue(con2Callback.get(5, TimeUnit.SECONDS),
            "Connection Established in Con Manager 2");
  }

  @Test
  void receiveMessage() throws Exception {
    CompletableFuture<Peer> con1PeerCallback = new CompletableFuture<>();
    con1PeerCallback.thenAccept(x -> {
      try {
        x.sendPeerMessage(new PingMessage(membraneAuthInfo1.getClientId(), membraneAuthInfo2.getClientId(), MembraneBuild.VERSION));
      } catch (PeerException e) {
        throw new Error(e);
      }
    });

    CompletableFuture<Peer> con2PeerCallback = new CompletableFuture<>();
    con2PeerCallback.thenAccept(x -> {
      try {
        x.sendPeerMessage(new PingMessage(membraneAuthInfo2.getClientId(), membraneAuthInfo1.getClientId(), MembraneBuild.VERSION));
      } catch (PeerException e) {
        throw new Error(e);
      }
    });

    CompletableFuture<PeerMessage> con1MessageCallback = new CompletableFuture<>();
    CompletableFuture<PeerMessage> con2MessageCallback = new CompletableFuture<>();

    connectionManager1.registerNewPeerCallback(con1PeerCallback::complete);
    connectionManager2.registerNewPeerCallback(con2PeerCallback::complete);

    connectionManager1.registerMessageCallback(con1MessageCallback::complete);
    connectionManager2.registerMessageCallback(con2MessageCallback::complete);

    connectionManager2.connectToPeer("127.0.0.1", 12450);

    Assertions.assertNotNull(con1MessageCallback.get(2, TimeUnit.SECONDS),
            "Connection Established in Con Manager 1");

    Assertions.assertNotNull(con2MessageCallback.get(2, TimeUnit.SECONDS),
            "Connection Established in Con Manager 2");

  }

  @Test
  void rejectMessageMasquerade() throws Exception {
    CompletableFuture<Peer> con1PeerCallback = new CompletableFuture<>();
    con1PeerCallback.thenAccept(x -> {
      try {
        x.sendPeerMessage(new PingMessage(TEST_SENDER, TEST_RECIPIENT, MembraneBuild.VERSION));
      } catch (PeerException e) {
        throw new Error(e);
      }
    });

    CompletableFuture<Peer> con2PeerCallback = new CompletableFuture<>();
    con2PeerCallback.thenAccept(x -> {
      try {
        x.sendPeerMessage(new PingMessage(TEST_SENDER, TEST_RECIPIENT, MembraneBuild.VERSION));
      } catch (PeerException e) {
        throw new Error(e);
      }
    });

    CompletableFuture<PeerMessage> con1MessageCallback = new CompletableFuture<>();
    CompletableFuture<PeerMessage> con2MessageCallback = new CompletableFuture<>();

    connectionManager1.registerNewPeerCallback(con1PeerCallback::complete);
    connectionManager2.registerNewPeerCallback(con2PeerCallback::complete);

    connectionManager1.registerMessageCallback(con1MessageCallback::complete);
    connectionManager2.registerMessageCallback(con2MessageCallback::complete);

    connectionManager2.connectToPeer("127.0.0.1", 12450);

    // Ensure no messages arrived within 5 seconds

    Assertions.assertThrows(TimeoutException.class, () -> con2MessageCallback.get(2, TimeUnit.SECONDS));
    Assertions.assertThrows(TimeoutException.class, () -> con1MessageCallback.get(2, TimeUnit.SECONDS));
  }

  @Test
  void singleConnectionOnlyTest() throws Exception {
    CompletableFuture<Peer> con1Callback_1 = new CompletableFuture<>();
    CompletableFuture<Peer> con1Callback_2 = new CompletableFuture<>();
    CompletableFuture<Peer> con1Callback_3 = new CompletableFuture<>();


    connectionManager1.registerNewPeerCallback(getPeerConsumer(con1Callback_1, con1Callback_2, con1Callback_3));

    CompletableFuture<Peer> con2Callback_1 = new CompletableFuture<>();
    CompletableFuture<Peer> con2Callback_2 = new CompletableFuture<>();
    CompletableFuture<Peer> con2Callback_3 = new CompletableFuture<>();

    connectionManager2.registerNewPeerCallback(getPeerConsumer(con2Callback_1, con2Callback_2, con2Callback_3));

    connectionManager2.connectToPeer("127.0.0.1", 12450);
    connectionManager1.connectToPeer("127.0.0.1", 12451);
    connectionManager2.connectToPeer("127.0.0.1", 12450);

    Assertions.assertNotNull(con1Callback_3.get(5, TimeUnit.SECONDS),
            "Connection Established 3 times in Con Manager 1");

    Assertions.assertNotNull(con2Callback_2.get(5, TimeUnit.SECONDS),
            "Connection Established 3 times in Con Manager 2");

    Assertions.assertEquals(1, connectionManager1.getAllConnectedPeers().size(), "Only one peer connection");
    Assertions.assertEquals(1, connectionManager2.getAllConnectedPeers().size(), "Only one peer connection");
  }

  @Test
  void cannotFindPeerTest() throws Exception {
    Assertions.assertThrows(TimeoutException.class, () -> connectionManager1.getPeerConnection(membraneAuthInfo2.getClientId(), 2, TimeUnit.SECONDS));
  }

  @Test
  void timeoutWaitingForReply() throws Exception {

    CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
      try {
        Peer peer = connectionManager1.getPeerConnection(membraneAuthInfo2.getClientId(), 2, TimeUnit.SECONDS);
        PongMessage pongMessage = new PongMessage(membraneAuthInfo1.getClientId(), TEST_RECIPIENT, MembraneBuild.VERSION, -1L);
        Assertions.assertThrows(TimeoutException.class, () -> peer.sendPeerMessageAndWait(pongMessage, 2, TimeUnit.SECONDS));
      } catch (TimeoutException e) {
        Assertions.fail("Could not find peer.");
      }
    });

    connectionManager2.connectToPeer("127.0.0.1", 12450);

    try {
      completableFuture.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      Assertions.fail("Timed out while waiting for connections to complete.");
    }
  }

  @Test
  void sendToClosedConnection() throws Exception {

    CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
      try {
        Peer peer = connectionManager1.getPeerConnection(membraneAuthInfo2.getClientId(), 2, TimeUnit.SECONDS);
        peer.close();
        while (!peer.isClosed()) {
          Thread.sleep(100);
        }
        PingMessage pingMessage = new PingMessage(membraneAuthInfo1.getClientId(), TEST_RECIPIENT, MembraneBuild.VERSION);
        Assertions.assertThrows(PeerException.class, () -> peer.sendPeerMessageAndWait(pingMessage, 2, TimeUnit.SECONDS));
      } catch (TimeoutException e) {
        Assertions.fail("Could not find peer.");
      } catch (InterruptedException e) {
        Assertions.fail("Waiting for connection close failed.");
      }
    });

    connectionManager2.connectToPeer("127.0.0.1", 12450);

    try {
      completableFuture.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      Assertions.fail("Timed out while waiting for connections to complete.");
    }
  }

  @Test
  void pingPongTest() throws Exception {
    PeerMessageActions peerMessageActionsCon1 = new PeerMessageActions(connectionManager1, membraneAuthInfo1.getClientId());
    PeerMessageActions peerMessageActionsCon2 = new PeerMessageActions(connectionManager2, membraneAuthInfo2.getClientId());

    connectionManager1.registerMessageCallback(peerMessage -> peerMessage.executeAction(peerMessageActionsCon1));
    connectionManager2.registerMessageCallback(peerMessage -> peerMessage.executeAction(peerMessageActionsCon2));

    // Connection 2 will send out ping and expect pong when conMan1 connects.

    CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
      try {
        Peer peer = connectionManager1.getPeerConnection(membraneAuthInfo2.getClientId(), 2, TimeUnit.SECONDS);
        PingMessage pingMessage = new PingMessage(membraneAuthInfo1.getClientId(), TEST_RECIPIENT, MembraneBuild.VERSION);
        try {
          peer.sendPeerMessageAndWait(pingMessage, 2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          Assertions.fail("Unsuccessful ping-pong: con1-PING->con2-PONG->con1. Did not return fast enough.");
        }
      } catch (TimeoutException e) {
        Assertions.fail("Unsuccessful ping-pong: con1-PING->con2-PONG->con1. Could not find peer.");
      } catch (PeerException e) {
        Assertions.fail("Unsuccessful ping-pong: con1-PING->con2-PONG->con1. Peer buffer full.");
      }
    });

    connectionManager2.connectToPeer("127.0.0.1", 12450);

    try {
      completableFuture.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      Assertions.fail("Timed out while waiting for connections to complete.");
    }
  }

  private Consumer<Peer> getPeerConsumer
          (CompletableFuture<Peer> con1Callback_1, CompletableFuture<Peer> con1Callback_2, CompletableFuture<Peer> con1Callback_3) {
    return peer -> {
      if (!con1Callback_1.isDone()) {
        con1Callback_1.complete(peer);
      } else if (!con1Callback_2.isDone()) {
        con1Callback_2.complete(peer);
      } else if (!con1Callback_3.isDone()) {
        con1Callback_3.complete(peer);
      }
    };
  }

  @AfterEach
  void tearDown() {
    connectionManager1.close();
    connectionManager2.close();
  }
}