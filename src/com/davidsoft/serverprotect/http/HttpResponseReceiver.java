package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.InputStream;

public final class HttpResponseReceiver {

    private final InputStream in;

    private HttpResponseInfo responseInfo;
    private InputStream analysedContentInputStream;

    private boolean chunkedTransfer;
    private HttpHeaders suspendedHeaders;

    public HttpResponseReceiver(InputStream in) {
        this.in = in;
    }

    public boolean receive() throws IOException {
        responseInfo = HttpResponseInfo.fromHttpStream(in);
        return responseInfo != null;
    }

    public static final int ANALYSE_SUCCESS = 0;
    public static final int ANALYSE_UNSUPPORTED_CONTENT_ENCODING = 1;
    public static final int ANALYSE_CONTENT_LENGTH_REQUIRED = 2;
    public static final int ANALYSE_MALFORMED_CONTENT_LENGTH = 3;

    public int analyseContent() throws IOException {
        long contentLength;
        String value = responseInfo.headers.getFieldValue("Transfer-Encoding");
        if ("chunked".equalsIgnoreCase(value)) {
            chunkedTransfer = true;
            contentLength = 0;
        }
        else {
            value = responseInfo.headers.getFieldValue("Content-Length");
            if (value == null) {
                value = responseInfo.headers.getFieldValue("Connection");
                if ("keep-alive".equalsIgnoreCase(value)) {
                    return ANALYSE_CONTENT_LENGTH_REQUIRED;
                }
                contentLength = HttpContentInputStream.CONTENT_LENGTH_UNKNOWN;
            }
            else {
                try {
                    contentLength = Long.parseLong(value);
                }
                catch (NumberFormatException e) {
                    return ANALYSE_MALFORMED_CONTENT_LENGTH;
                }
                if (contentLength < 0) {
                    return ANALYSE_MALFORMED_CONTENT_LENGTH;
                }
                if (contentLength == 0) {
                    return ANALYSE_SUCCESS;
                }
            }
        }
        analysedContentInputStream = HttpContentEncodedStreamFactory.instanceInputStream(
                new HttpContentInputStream(in, chunkedTransfer, contentLength),
                responseInfo.headers.getFieldValue("Content-Encoding")
        );
        if (analysedContentInputStream == null) {
            return ANALYSE_UNSUPPORTED_CONTENT_ENCODING;
        }
        return ANALYSE_SUCCESS;
    }

    public boolean hasContent() {
        return analysedContentInputStream != null;
    }

    public InputStream getContentInputStream() {
        return analysedContentInputStream;
    }

    public boolean hasSuspendedHeaders() {
        return chunkedTransfer;
    }

    public boolean readSuspendedHeaders() throws IOException {
        if (chunkedTransfer) {
            suspendedHeaders = HttpHeaders.fromResponseStream(in, new StringBuilder());
            return suspendedHeaders != null;
        }
        else {
            suspendedHeaders = new HttpHeaders();
            return true;
        }
    }

    public HttpResponseInfo getResponseInfo() {
        return responseInfo;
    }

    public HttpHeaders getSuspendedHeaders() {
        return suspendedHeaders;
    }
}
