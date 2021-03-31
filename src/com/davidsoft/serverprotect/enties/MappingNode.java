package com.davidsoft.serverprotect.enties;

public final class MappingNode {

    public int port;
    public boolean ssl;
    public MappingAppNode[] apps = new MappingAppNode[0];
    public String defaultApp = "";
}
