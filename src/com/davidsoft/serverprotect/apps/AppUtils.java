package com.davidsoft.serverprotect.apps;

import com.davidsoft.serverprotect.http.HttpRequestReceiver;
import com.davidsoft.serverprotect.http.HttpResponseInfo;
import com.davidsoft.serverprotect.http.HttpResponseSender;

public class AppUtils {

    public static HttpResponseSender requireContent(HttpRequestReceiver requestReceiver, String acceptableContentType) {
        if (!requestReceiver.hasContent()) {
            return new HttpResponseSender(new HttpResponseInfo(415), null);
        }
        String contentType = requestReceiver.getContentType();
        if (acceptableContentType != null && contentType != null && !contentType.equals(acceptableContentType)) {
            return new HttpResponseSender(new HttpResponseInfo(415), null);
        }
        return null;
    }
}
