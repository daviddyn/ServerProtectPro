package com.davidsoft.serverprotect.rulers;

import com.davidsoft.net.http.HttpRequestInfo;
import com.davidsoft.net.http.HttpResponseInfo;
import com.davidsoft.serverprotect.components.FrequencyManager;

public class FrequencyRuler implements Ruler {

    @Override
    public boolean judge(int clientIp, HttpRequestInfo requestInfo) {
        return FrequencyManager.query(clientIp, System.currentTimeMillis());
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
