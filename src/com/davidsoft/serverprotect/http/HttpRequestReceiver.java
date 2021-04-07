package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * 使用方法：
 * <p>1. 调用{@link #receive(int, int)}接收请求头, 根据返回的结果判断请求头语法是否正确、URL是否过长、请求头是否过大等。</p>
 * <p>2. 此后可以随时调用{@link #getRequestInfo()}函数获得请求头内容。</p>
 * <p>3. 调用{@link #analyseContent()}分析请求头，分析出此请求是否包含Content，若包含，则继续分析Transfer-Encoding、Content-Encoding、Content-Length、Connection等字段是否合理。</p>
 * <p>4. 此后可以随时调用{@link #hasContent()}判断此请求是否包含Content。若不包含Content，则至此Http请求已接收完成。</p>
 * <p>5. 此后可以随时调用{@link #hasSuspendedHeaders()}判断此请求是否包含悬挂头。注意：ChunkedTransfer一定包含悬挂头，即使悬挂头中没有任何字段；非ChunkedTransfer一定不包含悬挂头。此方法其实就是判断是否为ChunkedTransfer。</p>
 * <p>6. 调用{@link #getContentInputStream()}获得可以接收 解析后Content 的输入流。</p>
 * <p>7. 如果有悬挂头，<strong>则必须在getContentInputStream()返回的流读取至流末尾后，调用一次{@link #getSuspendedHeaders()}方法</strong>。</p>
 * <p>8. 此后可以随时调用{@link #getSuspendedHeaders()}获得悬挂头。</p>
 * </ul>
 */
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

    /**
     * 接收请求头数据。
     *
     * @param maxPathLength 最大URI字符数
     * @param maxHeaderSize 最大请求头字节数
     * @return {@link HttpRequestInfo#SUCCESS}、{@link HttpRequestInfo#INVALID_DATA}、{@link HttpRequestInfo#HEADER_SIZE_EXCEED}、{@link HttpRequestInfo#PATH_LENGTH_EXCEED}四者之一。
     *
     * @throws IOException 当发生IO异常。
     */
    public int receive(int maxPathLength, int maxHeaderSize) throws IOException {
        return requestInfo.fromHttpStream(in, maxPathLength, maxHeaderSize);
    }

    /**
     * Http请求头分析成功。
     */
    public static final int ANALYSE_SUCCESS = 0;
    /**
     * Content编码不受支持。目前仅支持不编码、gzip和deflate。
     */
    public static final int ANALYSE_UNSUPPORTED_CONTENT_ENCODING = 1;
    /**
     * 非ChunkedTransfer模式、复用连接的情况下未指定Content-Length字段。
     */
    public static final int ANALYSE_CONTENT_LENGTH_REQUIRED = 2;
    /**
     * 无法从Content-Length字段中解析到合法的非负整数值。
     */
    public static final int ANALYSE_MALFORMED_CONTENT_LENGTH = 3;

    /**
     * <p>分析请求头，分析出此请求是否包含Content，若包含，则继续分析Transfer-Encoding、Content-Encoding、Content-Length、Connection等字段是否合理。具体规则是：</p>
     * <p>1. 若Transfer-Encoding字段为Chunked，则忽略Content-Length字段。Connection字段默认为Keep-Alive</p>
     * <p>2. 若无Transfer-Encoding字段、有Content-Length字段，则Connection字段默认为Keep-Alive</p>
     * <p>3. 若无Transfer-Encoding字段、无Content-Length字段，则Connection字段默认为Closed，若Connection字段为Keep-Alive，则会返回{@link #ANALYSE_CONTENT_LENGTH_REQUIRED}</p>
     * <p>4. Content-Encoding字段仅支持不定义、gzip和deflate，否则会返回{@link #ANALYSE_UNSUPPORTED_CONTENT_ENCODING}</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #receive(int, int)}函数后调用。</p>
     *
     * @return {@link #ANALYSE_SUCCESS}、{@link #ANALYSE_UNSUPPORTED_CONTENT_ENCODING}、{@link #ANALYSE_CONTENT_LENGTH_REQUIRED}、{@link #ANALYSE_MALFORMED_CONTENT_LENGTH}四者之一。
     */
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

    /**
     * 判断此请求是否含有Content。需要在
     */
    public boolean hasContent() {
        return hasContent;
    }

    /**
     * 说明：Content-Encoding具有最低的优先级，即：
     * 1. 对于非ChunkedTransfer，Content-Length代表的是解码 <strong>前</strong> 的长度。
     * 2. 对于ChunkedTransfer，其每个Chunk的长度头所表示的长度是当前Chunk内容解码 <strong>前</strong> 的长度。将所有Chunk的内容拼接一起后再进行解码。
     */
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