package com.davidsoft.net.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HttpRequestSender {

	private static final byte[] CRLF_BYTES = "\r\n".getBytes(StandardCharsets.UTF_8);
	
    private final HttpRequestInfo requestInfo;
    private final HttpContentProvider contentProvider;

    public HttpRequestSender(HttpRequestInfo requestInfo, HttpContentProvider contentProvider) {
        this.requestInfo = requestInfo;
        this.contentProvider = contentProvider;
    }

    //注：content-encoding的信息从requestInfo中获取，content-type、content-length、transfer-encoding等字段会被从contentProvider覆盖。

    public void send(OutputStream out) throws IOException {
        boolean hasContent;
        DirectReferByteArrayOutputStream buffer = null;
        String contentEncoding = null;

        long contentLength = -1;
        if (contentProvider == null) {
            hasContent = false;
            requestInfo.headers.setFieldValue("Content-Length", "0");
        }
        else {
            if (contentProvider.useChunkedTransfer()) {
                requestInfo.headers.setFieldValue("Transfer-Encoding", "chunked");
                hasContent = true;
            }
            else {
                contentLength = contentProvider.getContentLength();
                if (contentLength == 0) {
                    requestInfo.headers.setFieldValue("Content-Length", "0");
                    hasContent = false;
                }
                else {
                    hasContent = true;
                }
            }
            if (hasContent) {
                contentEncoding = requestInfo.headers.getFieldValue("Content-Encoding");
                if (contentEncoding != null) {
                    if (!HttpContentEncodedStreamFactory.getSupportedContentEncodings().contains(contentEncoding)) {
                        throw new IllegalArgumentException("不支持的ContentEncoding：" + contentEncoding);
                    }
                }
                String value = contentProvider.getMimeType();
                if (value != null) {
                    Charset charset = contentProvider.getCharset();
                    if (charset == null) {
                        requestInfo.headers.setFieldValue("Content-Type", value);
                    }
                    else {
                        requestInfo.headers.setFieldValue("Content-Type", value + "; charset=" + charset.displayName());
                    }
                }
                if (!contentProvider.useChunkedTransfer()) {
                    if (contentEncoding == null) {
                        requestInfo.headers.setFieldValue("Content-Length", String.valueOf(contentLength));
                    }
                    else {
                        if (contentLength > Integer.MAX_VALUE - 8) {
                            throw new IOException("ContentLength过大，请使用ChunkedTransfer。");
                        }
                        else {
                            buffer = new DirectReferByteArrayOutputStream((int) contentLength);
                        }
                        EncodingOutputStream decorated = HttpContentEncodedStreamFactory.instanceOutputStream(buffer, contentEncoding);
                        contentProvider.onProvide(decorated);
                        decorated.close();
                        requestInfo.headers.setFieldValue("Content-Length", String.valueOf(buffer.size()));
                    }
                }
            }
        }
        requestInfo.toHttpStream(out);
        out.write(CRLF_BYTES);
        if (hasContent) {
            if (contentProvider.useChunkedTransfer()) {
                HttpChunkedOutputStream chunkedOut = new HttpChunkedOutputStream(out);
                EncodingOutputStream decorated = HttpContentEncodedStreamFactory.instanceOutputStream(chunkedOut, contentEncoding);
                contentProvider.onProvide(decorated);
                decorated.finish();
                chunkedOut.finish();
                HttpHeaders suspendedHeaders = contentProvider.getSuspendedHeader();
                if (suspendedHeaders != null) {
                    suspendedHeaders.toRequestStream(out);
                }
                out.write(CRLF_BYTES);
            }
            else {
                if (contentEncoding == null) {
                    contentProvider.onProvide(out);
                }
                else {
                    out.write(buffer.toByteArray(), 0, buffer.size());
                }
            }
        }
    }
}
