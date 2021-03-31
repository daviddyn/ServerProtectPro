package com.davidsoft.serverprotect.enties;

public final class PrecautionNode {

    public String method = "";
    public long lengthInMinute;
    public ActionNode action = new ActionNode();
    public ActionNode actionXhr = new ActionNode();
}
