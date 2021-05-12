package com.davidsoft.serverprotect.rulers;

import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;

public class MethodRuler implements Ruler {

    @Override
    public boolean judge(int clientIp, HttpRequestInfo requestInfo) {
        switch (requestInfo.method) {
            case "GET":
            case "POST":
            case "HEAD":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
