package com.davidsoft.serverprotect.rulers;

import com.davidsoft.http.HttpRequestInfo;
import com.davidsoft.http.HttpResponseInfo;

public class AgentRuler implements Ruler {
    @Override
    public boolean judge(String clientIp, HttpRequestInfo requestInfo) {
        String userAgent = requestInfo.headers.getFieldValue("User-Agent");
        if (userAgent == null) {
            return false;
        }
        //解析UserAgent
        if (!userAgent.startsWith("Mozilla/5.0 (")) {
            return false;
        }
        int findPos = userAgent.indexOf(") ", 13);
        if (findPos == -1) {
            return false;
        }
        findPos += 2;
        if (!userAgent.startsWith("AppleWebKit/537.36 (KHTML, like Gecko) ", findPos)) {
            return false;
        }
        //检查语法
        findPos += 39;
        userAgent = userAgent.substring(findPos);
        if (userAgent.length() == 0) {
            return false;
        }
        for (String pattern : userAgent.split(" ")) {
            if (pattern.length() == 0) {
                return false;
            }
            findPos = pattern.indexOf('/');
            if (findPos == -1) {
                continue;
            }
            if (findPos == 0 || findPos == pattern.length() - 1) {
                return false;
            }
            findPos = pattern.indexOf('/', findPos + 1);
            if (findPos != -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
