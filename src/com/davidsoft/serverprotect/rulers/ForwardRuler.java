package com.davidsoft.serverprotect.rulers;

import com.davidsoft.serverprotect.http.HttpRequestInfo;
import com.davidsoft.serverprotect.http.HttpResponseInfo;

public class ForwardRuler implements Ruler {

    @Override
    public boolean judge(String clientIp, HttpRequestInfo requestInfo) {
        String forward = requestInfo.headers.getFieldValue("X-Forwarded-For");
        if (forward == null) {
            return true;
        }
        int findPos = forward.indexOf(',');
        if (findPos == -1) {
            findPos = forward.length();
        }
        return clientIp.equals(forward.substring(0, findPos).trim());
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
