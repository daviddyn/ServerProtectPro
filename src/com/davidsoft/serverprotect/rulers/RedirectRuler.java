package com.davidsoft.serverprotect.rulers;

import com.davidsoft.serverprotect.components.TraceManager;
import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;

public class RedirectRuler implements Ruler {

    private boolean doSomething;

    @Override
    public boolean judge(int clientIp, HttpRequestInfo requestInfo) {
        String traceId = requestInfo.headers.cookies.get("traceId");
        if (traceId == null) {
            doSomething = true;
            return true;
        }
        doSomething = false;
        TraceManager.TraceInfo traceInfo = TraceManager.getTraceInfo(traceId);
        if (traceInfo == null || traceInfo.requiredRedirectLocation == null) {
            return true;
        }
        return traceInfo.requiredRedirectLocation.equals(requestInfo.uri);
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {
        if (doSomething) {

        }
    }
}
