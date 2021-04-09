package com.davidsoft.serverprotect.rulers;

import com.davidsoft.serverprotect.Utils;
import com.davidsoft.serverprotect.components.FrequencyManager;
import com.davidsoft.http.HttpRequestInfo;
import com.davidsoft.http.HttpResponseInfo;

public class FrequencyRuler implements Ruler {

    @Override
    public boolean judge(String clientIp, HttpRequestInfo requestInfo) {
        return FrequencyManager.query(Utils.encodeIp(clientIp), System.currentTimeMillis());
    }

    @Override
    public void onDoSomethingForResponse(HttpResponseInfo responseInfo) {

    }
}
