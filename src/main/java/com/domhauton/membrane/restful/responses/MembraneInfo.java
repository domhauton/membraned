package com.domhauton.membrane.restful.responses;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by dominic on 03/02/17.
 */
public class MembraneInfo implements MembraneResponse {
    private String name;
    private String hostname;
    private int port;
    private String version;
    private MembraneStatus status;
    private String tagline;

    public MembraneInfo(int port, String version, MembraneStatus status, String tagline) {
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.hostname = "unknown";
        }
        this.port = port;
        this.version = version;
        this.status = status;
        this.tagline = tagline;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getVersion() {
        return version;
    }

    public MembraneStatus getStatus() {
        return status;
    }

    public String getTagline() {
        return tagline;
    }
}
