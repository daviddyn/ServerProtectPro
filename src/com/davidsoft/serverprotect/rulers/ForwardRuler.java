package com.davidsoft.serverprotect.rulers;

import com.davidsoft.net.IP;
import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;

import java.text.ParseException;

public class ForwardRuler implements Ruler {

    @Override
    public boolean judge(int clientIp, HttpRequestInfo requestInfo) {
        String forward = requestInfo.headers.getFieldValue("X-Forwarded-For");
        if (forward == null) {
            return true;
        }
        int findPos = forward.indexOf(',');
        if (findPos == -1) {
            findPos = forward.length();
        }
        try {
            return clientIp == IP.parse(forward.substring(0, findPos).trim());
        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
