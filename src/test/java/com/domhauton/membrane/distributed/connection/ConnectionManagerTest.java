package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.auth.AuthUtils;
import com.domhauton.membrane.distributed.auth.MembraneAuthInfo;
import com.domhauton.membrane.distributed.connection.peer.Peer;
import com.domhauton.membrane.distributed.connection.peer.PeerException;
import com.domhauton.membrane.distributed.messaging.PeerMessage;
import com.domhauton.membrane.distributed.messaging.messages.PingMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by dominic on 13/02/17.
 */
class ConnectionManagerTest {
  private Logger logger = LogManager.getLogger();

  private MembraneAuthInfo membraneAuthInfo1;
  private MembraneAuthInfo membraneAuthInfo2;
  private ConnectionManager connectionManager1;
  private ConnectionManager connectionManager2;

  @BeforeEach
  void setUp() throws Exception {
    // System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
    //System.setProperty("javax.net.debug", "ssl");
    membraneAuthInfo1 = AuthUtils.generateAuthenticationInfo();
    membraneAuthInfo2 = AuthUtils.generateAuthenticationInfo();
  }

  @Test
  void listenAndClose() throws Exception {
    connectionManager1 = new ConnectionManager(membraneAuthInfo1, 12450);
    connectionManager1.close();
  }

  @Test
  void establishConnection() throws Exception {
    connectionManager1 = new ConnectionManager(membraneAuthInfo1, 12450);
    connectionManager2 = new ConnectionManager(membraneAuthInfo2, 12451);

    CompletableFuture<Boolean> con1Callback = new CompletableFuture<>();
    CompletableFuture<Boolean> con2Callback = new CompletableFuture<>();

    connectionManager1.registerNewPeerCallback(peer -> con1Callback.complete(true));
    connectionManager2.registerNewPeerCallback(peer -> con2Callback.complete(true));

    connectionManager2.connectToPeer("127.0.0.1", 12450);

    Assertions.assertTrue(con1Callback.get(5, TimeUnit.SECONDS),
            "Connection Established in Con Manager 1");

    Assertions.assertTrue(con2Callback.get(5, TimeUnit.SECONDS),
            "Connection Established in Con Manager 2");

    connectionManager1.close();
    connectionManager2.close();
  }

  @Test
  void receiveMessage() throws Exception {
    connectionManager1 = new ConnectionManager(membraneAuthInfo1, 12450);
    connectionManager2 = new ConnectionManager(membraneAuthInfo2, 12451);

    CompletableFuture<Peer> con1PeerCallback = new CompletableFuture<>();
    con1PeerCallback.thenAccept(x -> {
      try {
        x.sendPeerMessage(new PingMessage());
      } catch (PeerException e) {
        throw new Error(e);
      }
    });

    CompletableFuture<Peer> con2PeerCallback = new CompletableFuture<>();
    con2PeerCallback.thenAccept(x -> {
      try {
        x.sendPeerMessage(new PingMessage());
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

    Assertions.assertNotNull(con1MessageCallback.get(5, TimeUnit.SECONDS),
            "Connection Established in Con Manager 1");

    Assertions.assertNotNull(con2MessageCallback.get(5, TimeUnit.SECONDS),
            "Connection Established in Con Manager 2");

    connectionManager1.close();
    connectionManager2.close();
  }

  @Test
  void singleConnectionOnlyTest() throws Exception {
    connectionManager1 = new ConnectionManager(membraneAuthInfo1, 12450);
    connectionManager2 = new ConnectionManager(membraneAuthInfo2, 12451);

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

    Assertions.assertTrue(con1Callback_1.get().isClosed());
    Assertions.assertTrue(con1Callback_2.get().isClosed());
    Assertions.assertTrue(con2Callback_1.get().isClosed());
    Assertions.assertTrue(con2Callback_2.get().isClosed());

    connectionManager1.close();
    connectionManager2.close();
  }

  private Consumer<Peer> getPeerConsumer(CompletableFuture<Peer> con1Callback_1, CompletableFuture<Peer> con1Callback_2, CompletableFuture<Peer> con1Callback_3) {
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
}