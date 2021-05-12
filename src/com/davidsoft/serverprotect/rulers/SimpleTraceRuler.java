package com.davidsoft.serverprotect.rulers;

import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;
import com.davidsoft.serverprotect.components.Settings;
import com.davidsoft.url.URIIndex;

public class SimpleTraceRuler implements Ruler {

    @Override
    public boolean judge(int clientIp, HttpRequestInfo requestInfo) {
        if ("favicon.ico".equals(requestInfo.uri.getResourceName())) {
            return true;
        }
        URIIndex.QueryResult<Boolean> queryResult = Settings.getRuntimeSettings().protections.traceURIs.get(requestInfo.uri);
        if (queryResult == null) {
            return false;
        }
        return queryResult.matchedExactly || queryResult.data;
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
