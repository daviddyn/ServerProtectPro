package com.davidsoft.serverprotect.rulers;

import com.davidsoft.http.HttpRequestInfo;
import com.davidsoft.http.HttpResponseInfo;

public interface Ruler {

    boolean judge(String clientIp, HttpRequestInfo requestInfo);

    void onDoSomethingForResponse(HttpResponseInfo responseInfo);
}
