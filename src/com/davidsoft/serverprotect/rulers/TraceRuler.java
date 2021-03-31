package com.davidsoft.serverprotect.rulers;

import com.davidsoft.serverprotect.http.HttpRequestInfo;
import com.davidsoft.serverprotect.http.HttpResponseInfo;

public class TraceRuler implements Ruler {

    @Override
    public boolean judge(String clientIp, HttpRequestInfo requestInfo) {
        //TODO: 路径判断算法
        return true;
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
