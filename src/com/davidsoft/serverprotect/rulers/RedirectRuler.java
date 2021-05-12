package com.davidsoft.serverprotect.rulers;

import com.davidsoft.serverprotect.components.TraceManager;
import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;

import java.util.UUID;

public class RedirectRuler implements Ruler {

    private boolean newer;

    @Override
    public boolean judge(int clientIp, HttpRequestInfo requestInfo) {
        String traceId = requestInfo.headers.cookies.get("traceId");
        if (traceId == null) {
            newer = true;
            return true;
        }
        newer = false;
        TraceManager.TraceInfo traceInfo = TraceManager.getTraceInfo(traceId);
        if (traceInfo == null || traceInfo.requiredRedirectLocation == null) {
            return true;
        }
        return traceInfo.requiredRedirectLocation.equals(requestInfo.uri);
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {
        if (responseInfo.responseCode == 302) {

        }
        if (newer) {
            String traceId = UUID.randomUUID().toString();
            responseInfo.headers.cookies.put("traceId", traceId);
        }
    }
}
