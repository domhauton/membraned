package com.domhauton.membrane.distributed.connection;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

/**
 * Created by dominic on 08/02/17.
 */
public class PeerConnection {
    private NetSocket netSocket;

    public PeerConnection(NetSocket netSocket) {
        this.netSocket = netSocket;
    }

    public void sendData(byte[] data) {
        Buffer writeBuffer = Buffer.buffer(data);
        this.netSocket.write(writeBuffer);
    }

    public void readData(byte[] data) {

    }
}
