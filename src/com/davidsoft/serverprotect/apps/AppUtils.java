package com.davidsoft.serverprotect.apps;

import com.davidsoft.serverprotect.http.HttpRequestReceiver;
import com.davidsoft.serverprotect.http.HttpResponseInfo;
import com.davidsoft.serverprotect.http.HttpResponseSender;

public class AppUtils {

    public static HttpResponseSender analyseRequestContent(HttpRequestReceiver requestReceiver) {
        switch (requestReceiver.analyseContent()) {
            case HttpRequestReceiver.ANALYSE_SUCCESS:
                return null;
            case HttpRequestReceiver.ANALYSE_UNSUPPORTED_CONTENT_ENCODING:
                return new HttpResponseSender(new HttpResponseInfo(501), null);
            case HttpRequestReceiver.ANALYSE_CONTENT_LENGTH_REQUIRED:
                return new HttpResponseSender(new HttpResponseInfo(411), null);
            case HttpRequestReceiver.ANALYSE_MALFORMED_CONTENT_LENGTH:
                return new HttpResponseSender(new HttpResponseInfo(400), null);
            default:
                return new HttpResponseSender(new HttpResponseInfo(500), null);
        }
    }

    public static HttpResponseSender analyseRequestContent(HttpRequestReceiver requestReceiver, String acceptableContentType) {
        HttpResponseSender response = analyseRequestContent(requestReceiver);
        if (response != null) {
            return response;
        }
        String contentType = requestReceiver.getContentType();
        if (contentType != null && !contentType.equals(acceptableContentType)) {
            return new HttpResponseSender(new HttpResponseInfo(415), null);
        }
        return null;
    }
}
