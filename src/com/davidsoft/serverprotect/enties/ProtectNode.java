package com.davidsoft.serverprotect.enties;

public final class ProtectNode {
    public PrecautionNode blockAction;
    public ConfigNode config;
    public PrecautionNode illegalAgent;
    public PrecautionNode illegalData;
    public PrecautionNode illegalForward;
    public PrecautionNode illegalFreq;
    public PrecautionNode illegalMethod;
    public PrecautionNode illegalRedirect;
    public PrecautionNode illegalTrace;
    public PathsNode paths;
}
