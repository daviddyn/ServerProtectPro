package com.davidsoft.serverprotect.rulers;

import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;

public interface Ruler {

    boolean judge(int clientIp, HttpRequestInfo requestInfo);

    void onDoSomethingForResponse(HttpResponseInfo responseInfo);
}
