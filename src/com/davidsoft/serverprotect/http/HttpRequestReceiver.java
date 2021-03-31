package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public final class HttpRequestReceiver {

    private final InputStream in;

    private final HttpRequestInfo requestInfo;
    private InputStream analysedContentInputStream;

    private boolean hasContent;

    private long contentLength;
    private String contentType;
    private Charset contentCharset;
    private boolean chunkedTransfer;
    private HttpHeaders suspendedHeaders;

    public HttpRequestReceiver(InputStream in) {
        requestInfo = new HttpRequestInfo();
        this.in = in;
    }

    public int receive(int maxPathLength, int maxHeaderSize) throws IOException {
        return requestInfo.fromHttpStream(in, maxPathLength, maxHeaderSize);
    }

    public static final int ANALYSE_SUCCESS = 0;
    public static final int ANALYSE_UNSUPPORTED_CONTENT_ENCODING = 1;
    public static final int ANALYSE_CONTENT_LENGTH_REQUIRED = 2;
    public static final int ANALYSE_MALFORMED_CONTENT_LENGTH = 3;

    public int analyseContent() {
        if (!"POST".equals(requestInfo.method)) {
            return ANALYSE_SUCCESS;
        }
        String value = requestInfo.headers.getFieldValue("Content-Type");
        if (value != null) {
            int findPos = value.indexOf(";");
            if (findPos == -1) {
                contentType = value.trim();
            }
            else {
                contentType = value.substring(0, findPos).trim();
                value = value.substring(findPos + 1).trim();
                if (value.startsWith("charset=")) {
                    contentCharset = Charset.forName(value.substring(8).trim());
                }
            }
        }

        value = requestInfo.headers.getFieldValue("Transfer-Encoding");
        if ("chunked".equalsIgnoreCase(value)) {
            chunkedTransfer = true;
            contentLength = HttpContentInputStream.CONTENT_LENGTH_UNKNOWN;
        }
        else {
            value = requestInfo.headers.getFieldValue("Content-Length");
            if (value == null) {
                value = requestInfo.headers.getFieldValue("Connection");
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
        if (!HttpContentEncodedStreamFactory.getSupportedContentEncodings().contains(requestInfo.headers.getFieldValue("Content-Encoding"))) {
            return ANALYSE_UNSUPPORTED_CONTENT_ENCODING;
        }
        hasContent = true;
        return ANALYSE_SUCCESS;
    }

    public boolean hasContent() {
        return hasContent;
    }

    public InputStream getContentInputStream() throws IOException {
        if (!hasContent) {
            return null;
        }
        if (analysedContentInputStream == null) {
            analysedContentInputStream = HttpContentEncodedStreamFactory.instanceInputStream(
                    new HttpContentInputStream(in, chunkedTransfer, contentLength),
                    requestInfo.headers.getFieldValue("Content-Encoding")
            );
        }
        return analysedContentInputStream;
    }

    public InputStream getRawInputStream() {
        return in;
    }

    public boolean hasSuspendedHeaders() {
        return chunkedTransfer;
    }

    public boolean readSuspendedHeaders() throws IOException {
        if (chunkedTransfer) {
            suspendedHeaders = HttpHeaders.fromRequestStream(in, new StringBuilder());
            return suspendedHeaders != null;
        }
        else {
            suspendedHeaders = new HttpHeaders();
            return true;
        }
    }

    public HttpRequestInfo getRequestInfo() {
        return requestInfo;
    }

    public HttpHeaders getSuspendedHeaders() {
        return suspendedHeaders;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public Charset getContentCharset() {
        return contentCharset;
    }
}