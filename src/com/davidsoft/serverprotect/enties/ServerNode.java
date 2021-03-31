package com.davidsoft.serverprotect.enties;

import java.util.Properties;

public final class ServerNode {

    public int maxConnections;
    public int maxServices;
    public boolean keepConnections;
    public int maxPathLength;
    public int maxHeaderSize;

    public ServerNode() {
        maxConnections = 1024;
        maxServices = 512;
        keepConnections = true;
        maxPathLength = 256;
        maxHeaderSize = 5120;
    }

    public ServerNode(Properties properties) {
        maxConnections = Integer.parseInt(properties.getProperty("maxConnections"));
        maxServices = Integer.parseInt(properties.getProperty("maxServices"));
        keepConnections = Boolean.parseBoolean(properties.getProperty("keepConnections"));
        maxPathLength = Integer.parseInt(properties.getProperty("maxPathLength"));
        maxHeaderSize = Integer.parseInt(properties.getProperty("maxHeaderSize"));
    }
}
