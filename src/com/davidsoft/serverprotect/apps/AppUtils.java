package com.davidsoft.serverprotect.apps;

import com.davidsoft.net.http.HttpContentReceiver;
import com.davidsoft.net.http.HttpResponseInfo;
import com.davidsoft.net.http.HttpResponseSender;

public class AppUtils {

    public static HttpResponseSender requireContent(HttpContentReceiver contentReceiver, String acceptableContentType) {
        if (contentReceiver == null || !contentReceiver.hasContent()) {
            return new HttpResponseSender(new HttpResponseInfo(415), null);
        }
        String contentType = contentReceiver.getContentType();
        if (acceptableContentType != null && contentType != null && !contentType.equals(acceptableContentType)) {
            return new HttpResponseSender(new HttpResponseInfo(415), null);
        }
        return null;
    }
}
