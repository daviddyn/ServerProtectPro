package com.davidsoft.serverprotect.rulers;

import com.davidsoft.http.HttpRequestInfo;
import com.davidsoft.http.HttpResponseInfo;

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
