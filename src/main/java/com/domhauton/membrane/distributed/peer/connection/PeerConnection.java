package com.domhauton.membrane.distributed.peer.connection;

import com.domhauton.membrane.distributed.messaging.PeerMessage;
import com.domhauton.membrane.distributed.peer.PeerException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.Optional.of;

/**
 * Created by dominic on 08/02/17.
 */
public class PeerConnection {
    private final static int AUTHENTICATION_TIMEOUT_SEC = 20;
    private final Logger logger = LogManager.getLogger();
    private NetSocket netSocket;
    private DateTime connectionStartTime;
    private Buffer writeBuffer;
    private Set<Consumer<PeerMessage>> eventConsumer;
    private CompletableFuture<String> completableFutureUID;

    private boolean isClosed;

    public PeerConnection(NetSocket netSocket) {
        isClosed = false;
        connectionStartTime = DateTime.now();
        completableFutureUID = new CompletableFuture<>();
        this.netSocket = netSocket;
        this.writeBuffer = Buffer.buffer();
        this.netSocket.handler(this::eventFilter);
    }

    public void sendData(PeerMessage peerMessage) throws PeerException {
        Buffer writeBuffer = Buffer.buffer(peerMessage.getBytes());
        if(!netSocket.writeQueueFull()) {
            netSocket.write(writeBuffer);
        } else {
            throw new PeerException("Write queue was full.");
        }
    }

    public void eventFilter(Buffer buffer) {
        PeerMessage peerMessage = null; //FIXME PARSE BUFFER TO MESSAGE
        //TODO CHECK Authentication matches.
        if(completableFutureUID.isDone()) {
            // Check authentication
        } else {
            // Authentication should be included in message.
            // Check message self-authenticates. (Prove they actually own the key)
            // Set the new key.
            completableFutureUID.complete("foobarkey"); //FIXME This is NOT PRODUCTION
        }
    }

    public void setMessageConsumer(Consumer<byte[]> messageConsumer) {
    }

    public synchronized Optional<String> authenticate() {
        PeerMessage authMessage = null; //FIXME Write auth message
        Buffer messageBuffer = Buffer.buffer(authMessage.getBytes());
        this.netSocket.write(messageBuffer);
        try {
            return of(completableFutureUID.get(AUTHENTICATION_TIMEOUT_SEC, TimeUnit.SECONDS));
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Authentication wait for address failed. {}",
                    netSocket.remoteAddress(), e.getMessage());
            close();
            return Optional.empty();
        } catch (TimeoutException e) {
            logger.error("Authentication timeout for {} after {} seconds.",
                    netSocket.remoteAddress(), AUTHENTICATION_TIMEOUT_SEC);
            close();
            return Optional.empty();
        }
    }

    public String getIP() {
        return netSocket.remoteAddress().host();
    }

    public int getPort() {
        return netSocket.remoteAddress().port();
    }

    public synchronized boolean isAuthenticated() {
        return completableFutureUID.isDone() && !isClosed;
    }

    public synchronized void close() {
        isClosed = true;
        netSocket.close();
    }
}
