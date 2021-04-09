package com.davidsoft.serverprotect.rulers;

import com.davidsoft.serverprotect.components.TraceManager;
import com.davidsoft.http.HttpRequestInfo;
import com.davidsoft.http.HttpResponseInfo;

public class RedirectRuler implements Ruler {

    private boolean doSomething;

    @Override
    public boolean judge(String clientIp, HttpRequestInfo requestInfo) {
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
        return traceInfo.requiredRedirectLocation.equals(requestInfo.path.toString());
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {
        if (doSomething) {

        }
    }
}
