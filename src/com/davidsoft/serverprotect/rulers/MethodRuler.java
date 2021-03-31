package com.davidsoft.serverprotect.rulers;

import com.davidsoft.serverprotect.http.HttpRequestInfo;
import com.davidsoft.serverprotect.http.HttpResponseInfo;

public class MethodRuler implements Ruler {

    @Override
    public boolean judge(String clientIp, HttpRequestInfo requestInfo) {
        switch (requestInfo.method) {
            case "GET":
            case "POST":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
