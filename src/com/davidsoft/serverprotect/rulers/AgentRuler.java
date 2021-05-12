package com.davidsoft.serverprotect.rulers;

import com.davidsoft.net.Origin;
import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;

import java.text.ParseException;

public class AgentRuler implements Ruler {

    @Override
    public boolean judge(int clientIp, HttpRequestInfo requestInfo) {
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
        //检查UserAgent语法
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
        //如果是POST请求，则必须包含origin字段，且格式正确
        if ("POST".equals(requestInfo.method)) {
            String origin = requestInfo.headers.getFieldValue("Origin");
            if (origin == null) {
                return false;
            }
            try {
                Origin.parse(origin);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
