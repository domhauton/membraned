package com.domhauton.membrane.distributed.connection;

import com.domhauton.membrane.distributed.connection.peer.PeerException;
import com.domhauton.membrane.distributed.messaging.PeerMessage;
import com.domhauton.membrane.distributed.messaging.PeerMessageUtils;
import com.google.common.hash.Hashing;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.CertificateEncodingException;
import javax.security.cert.X509Certificate;
import java.util.function.Consumer;

/**
 * Created by dominic on 08/02/17.
 */
public class PeerConnection {
    private final Logger logger = LogManager.getLogger();
    private final X509Certificate x509Certificate;
    private final String clientID;
    private final NetSocket netSocket;
    private final Consumer<PeerMessage> messageConsumer;
    private final DateTime connectionStartTime;

    public PeerConnection(NetSocket netSocket, Consumer<PeerMessage> messageConsumer) throws PeerException {
        connectionStartTime = DateTime.now();
        this.netSocket = netSocket;
        this.messageConsumer = messageConsumer;
        this.netSocket.handler(this::messageHandler);
        try {
            X509Certificate[] certificates = netSocket.peerCertificateChain();
            if (certificates != null && certificates.length == 1) {
                x509Certificate = certificates[0];
                clientID = Hashing.md5().hashBytes(x509Certificate.getEncoded()).toString();
            } else {
                logger.error("Certificate count invalid for {}. Dropping connection.", netSocket.localAddress());
                netSocket.close();
                throw new PeerException("Connection had incorrect number of certificates. Dropping");
            }
        } catch (SSLPeerUnverifiedException e) {
            logger.error("Connection unverified. Dropping.");
            netSocket.close();
            throw new PeerException("Connection could not be verified. Dropping.");
        } catch (CertificateEncodingException e) {
            logger.error("Invalid certificated received. Could not generate client ID");
            netSocket.close();
            throw new PeerException("Connection could not parse certificate. Dropping.");
        }
    }

    public void sendData(PeerMessage peerMessage) throws PeerException {
        Buffer writeBuffer = Buffer.buffer(peerMessage.getBytes());
        if (!netSocket.writeQueueFull()) {
            logger.trace("Sending data from client [{}]: ", clientID, peerMessage);
            netSocket.write(writeBuffer);
        } else {
            throw new PeerException("Write queue was full.");
        }
    }

    private void messageHandler(Buffer buffer) {
        PeerMessage peerMessage = PeerMessageUtils.parseMessage(buffer.getBytes());
        messageConsumer.accept(peerMessage);
    }

    public String getIP() {
        return netSocket.remoteAddress().host();
    }

    public int getPort() {
        return netSocket.remoteAddress().port();
    }

    public String getClientID() {
        return clientID;
    }

    public DateTime getConnectionStartTime() {
        return connectionStartTime;
    }

    public synchronized void close() {
        netSocket.close();
    }
}
