package com.davidsoft.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HttpResponseSender {

    public final HttpResponseInfo responseInfo;
    private final HttpContentProvider contentProvider;

    public HttpResponseSender(HttpResponseInfo responseInfo, HttpContentProvider contentProvider) {
        this.responseInfo = responseInfo;
        this.contentProvider = contentProvider;
    }

    //注：content-encoding的信息从requestInfo中获取，content-type、content-length、transfer-encoding等字段会被从contentProvider覆盖。

    public void send(OutputStream out, String contentEncoding) throws IOException {
        boolean hasContent;
        DirectReferByteArrayOutputStream buffer = null;

        long contentLength = -1;
        if (contentProvider == null) {
            hasContent = false;
            responseInfo.headers.setFieldValue("Content-Length", "0");
        }
        else {
            if (contentProvider.useChunkedTransfer()) {
                responseInfo.headers.setFieldValue("Transfer-Encoding", "chunked");
                hasContent = true;
            }
            else {
                contentLength = contentProvider.getContentLength();
                if (contentLength == 0) {
                    responseInfo.headers.setFieldValue("Content-Length", "0");
                    hasContent = false;
                }
                else {
                    hasContent = true;
                }
            }
            if (hasContent) {
                if (contentEncoding != null) {
                    if (!HttpContentEncodedStreamFactory.getSupportedContentEncodings().contains(contentEncoding)) {
                        throw new IllegalArgumentException("不支持的ContentEncoding：" + contentEncoding);
                    }
                    responseInfo.headers.setFieldValue("Content-Encoding", contentEncoding);
                }
                String value = contentProvider.getMimeType();
                if (value != null) {
                    Charset charset = contentProvider.getCharset();
                    if (charset == null) {
                        responseInfo.headers.setFieldValue("Content-Type", value);
                    }
                    else {
                        responseInfo.headers.setFieldValue("Content-Type", value + "; charset=" + charset.displayName());
                    }
                }
                if (!contentProvider.useChunkedTransfer()) {
                    if (contentEncoding == null) {
                        responseInfo.headers.setFieldValue("Content-Length", String.valueOf(contentLength));
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
                        responseInfo.headers.setFieldValue("Content-Length", String.valueOf(buffer.size()));
                    }
                }
            }
        }
        responseInfo.toHttpStream(out);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
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
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
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
