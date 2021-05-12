package com.davidsoft.serverprotect.rulers;

import com.davidsoft.net.NetURI;
import com.davidsoft.serverprotect.components.TraceManager;
import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;

import java.util.UUID;

public class RedirectRuler implements Ruler {

    private String traceId;

    @Override
    public boolean judge(int clientIp, HttpRequestInfo requestInfo) {
        traceId = requestInfo.headers.cookies.get("traceId");
        if (traceId == null) {
            return true;
        }
        TraceManager.TraceInfo traceInfo = TraceManager.getTraceInfo(traceId);
        if (traceInfo == null || traceInfo.requiredRedirectLocation == null) {
            return true;
        }
        return traceInfo.requiredRedirectLocation.equals(NetURI.toString(requestInfo.uri));
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            responseInfo.headers.cookies.put("traceId", "traceId=" + traceId + "; path=/");
        }
        if (responseInfo.responseCode == 302) {
            TraceManager.registerRedirect(traceId, responseInfo.headers.getFieldValue("location"));
        }
        else {
            TraceManager.registerRedirect(traceId, null);
        }
    }
}
