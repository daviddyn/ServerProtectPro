package com.davidsoft.serverprotect.rulers;

import com.davidsoft.serverprotect.http.HttpRequestInfo;
import com.davidsoft.serverprotect.http.HttpResponseInfo;

public interface Ruler {

    boolean judge(String clientIp, HttpRequestInfo requestInfo);

    void onDoSomethingForResponse(HttpResponseInfo responseInfo);
}
