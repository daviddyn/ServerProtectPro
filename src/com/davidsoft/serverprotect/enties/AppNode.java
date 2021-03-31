package com.davidsoft.serverprotect.enties;

public final class AppNode {

    public String name = "";
    public String type = "";
    public String[] whiteList = new String[0];
    public String[] domains = new String[0];
    public RouterNode[] routers = new RouterNode[0];
    public ForwardTargetNode target = new ForwardTargetNode();
}
