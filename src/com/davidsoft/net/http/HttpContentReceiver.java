package com.davidsoft.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * 大致使用方法：
 * <p>1. 调用{@link #analyseContent()}分析请求头，分析出此请求是否包含Content，若包含，则继续分析Transfer-Encoding、Content-Encoding、Content-Length、Connection等字段是否合理。</p>
 * <p>2. 调用{@link #hasContent()}判断此请求是否包含Content。若不包含Content，则至此Http请求已接收完成。</p>
 * <p>3. 调用{@link #getContentInputStream()}获得接收Content的输入流。</p>
 * <p>4. 调用{@link #hasSuspendedHeaders()}判断此请求是否包含悬挂头。若不包含Content，则至此Http请求已接收完成。</p>
 * <p>5. 调用{@link #getSuspendedHeaders()}接收悬挂头。</p>
 */
public class HttpContentReceiver {

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

    private final InputStream in;
    private final HttpHeaders httpHeaders;

    private boolean hasContent;

    private long contentLength;
    private String contentType;
    private Charset contentCharset;
    private boolean chunkedTransfer;
    private HttpHeaders suspendedHeaders;

    public HttpContentReceiver(InputStream in, HttpHeaders httpHeaders) {
        this.in = in;
        this.httpHeaders = httpHeaders;
    }

    /**
     * <p>分析请求头，分析出此请求是否包含Content，若包含，则继续分析Transfer-Encoding、Content-Encoding、Content-Length、Connection等字段是否合理。具体规则是：</p>
     * <p>1. 若Transfer-Encoding字段为Chunked，则忽略Content-Length字段。Connection字段默认为Keep-Alive</p>
     * <p>2. 若无Transfer-Encoding字段、有Content-Length字段，则Connection字段默认为Keep-Alive</p>
     * <p>3. 若无Transfer-Encoding字段、无Content-Length字段，则Connection字段默认为Closed，若Connection字段为Keep-Alive，则会返回{@link #ANALYSE_CONTENT_LENGTH_REQUIRED}</p>
     * <p>4. Content-Encoding字段仅支持不定义、gzip和deflate，否则会返回{@link #ANALYSE_UNSUPPORTED_CONTENT_ENCODING}</p>
     *
     * @return {@link #ANALYSE_SUCCESS}、{@link #ANALYSE_UNSUPPORTED_CONTENT_ENCODING}、{@link #ANALYSE_CONTENT_LENGTH_REQUIRED}、{@link #ANALYSE_MALFORMED_CONTENT_LENGTH}四者之一。
     */
    public int analyseContent() {
        String value = httpHeaders.getFieldValue("Content-Type");
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

        value = httpHeaders.getFieldValue("Transfer-Encoding");
        if ("chunked".equalsIgnoreCase(value)) {
            chunkedTransfer = true;
            contentLength = HttpContentInputStream.CONTENT_LENGTH_UNKNOWN;
        }
        else {
            value = httpHeaders.getFieldValue("Content-Length");
            if (value == null) {
                value = httpHeaders.getFieldValue("Connection");
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
        if (!HttpContentEncodedStreamFactory.getSupportedContentEncodings().contains(httpHeaders.getFieldValue("Content-Encoding"))) {
            return ANALYSE_UNSUPPORTED_CONTENT_ENCODING;
        }
        hasContent = true;
        return ANALYSE_SUCCESS;
    }

    /**
     * <p>判断此请求是否含有Content。</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #analyseContent()}函数后调用。</p>
     */
    public boolean hasContent() {
        return hasContent;
    }

    /**
     * <p>获得用于读取请求Content的输入流。</p>
     * <p>返回的输入流已经根据请求头中的Transfer-Encoding、Content-Encoding、Content-Length和Connection等字段进行了处理。直接从此输入流读取数据到流末尾，即可得到全部的经解析后的真实Content数据。</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #analyseContent()}函数后调用。</p>
     * <p></p>
     * <p>备注：Content-Encoding具有最低的优先级，即：</p>
     * <p>1. 对于非ChunkedTransfer，Content-Length代表的是解码 <strong>前</strong> 的长度。</p>
     * <p>2. 对于ChunkedTransfer，其每个Chunk头所表示的长度是当前Chunk内容解码 <strong>前</strong> 的长度。将所有Chunk的内容拼接一起后再进行解码。</p>
     *
     * @return 用于读取请求Content的输入流。如果此请求没有Content，即{@link #hasContent()}返回{@code false}，则函数返回{@code null}。
     */
    public InputStream getContentInputStream() throws IOException {
        if (hasContent) {
            return HttpContentEncodedStreamFactory.instanceInputStream(
                    new HttpContentInputStream(in, chunkedTransfer, contentLength),
                    httpHeaders.getFieldValue("Content-Encoding")
            );
        }
        return null;
    }

    /**
     * <p>获得用于读取请求Content原始数据的输入流。</p>
     * <p>返回的流仅具备检测Content是否已读完的功能，并不会根据Transfer-Encoding和Content-Encoding对内容进行解码。</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #analyseContent()}函数后调用。</p>
     *
     * @return 用于读取请求Content的输入流。如果此请求没有Content，即{@link #hasContent()}返回{@code false}，则函数返回{@code null}。
     */
    public InputStream getContentRawInputStream() throws IOException {
        if (hasContent) {
            return new HttpContentRawInputStream(in, chunkedTransfer, contentLength);
        }
        return null;
    }

    /**
     * <p>判断此请求是否含有悬挂头。</p>
     * <p>悬挂头仅且必存在于ChunkedTransfer中，一般用于声明那些浏览器只有将Content内容全部发送给服务器后才会知晓的信息(如 Content-MD5)。悬挂头的语法格式与普通的请求头相同。</p>
     * <p>ChunkedTransfer一定包含悬挂头，即使悬挂头中没有任何字段；非ChunkedTransfer一定不包含悬挂头。此方法其实就是判断是否为ChunkedTransfer。</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #analyseContent()}函数后调用。</p>
     */
    public boolean hasSuspendedHeaders() {
        return chunkedTransfer;
    }

    /**
     * <p>接收悬挂头数据。</p>
     * <p></p>
     * <p>注意：</p>
     * <p>1. 如果{@code hasSuspendedHeaders()}返回{@code false}，则不可调用此方法。</p>
     * <p>2. 如果{@code hasSuspendedHeaders()}返回{@code true}，则<strong>当从{@code getContentInputStream()}返回的输入流读取至流末尾后，必须调用一次本方法</strong>。</p>
     *
     * @return {@link HttpHeaders#SUCCESS}、{@link HttpHeaders#INVALID_DATA}、{@link HttpHeaders#HEADER_SIZE_EXCEED}三者之一。
     *
     * @see #hasSuspendedHeaders()
     * @see #getContentInputStream()
     */
    public int receiveSuspendedHeaders() throws IOException {
        if (!chunkedTransfer) {
            throw new IllegalStateException("当前的Http请求不包含悬挂头。");
        }
        suspendedHeaders = new HttpHeaders();
        return suspendedHeaders.fromRequestStreamLimited(in, new StringBuilder(), Integer.MAX_VALUE);
    }

    /**
     * <p>获得此Http请求的悬挂头。</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #receiveSuspendedHeaders()}函数后调用。</p>
     */
    public HttpHeaders getSuspendedHeaders() {
        return suspendedHeaders;
    }

    /**
     * <p>获得此Http请求的Content-Length。</p>
     * <p>只有当{@link #hasContent()}返回{@code true}时，此函数的返回结果才有意义。</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #analyseContent()}函数后调用。</p>
     *
     * @return 此Http请求的Content-Length。当此Http请求未指定Content-Length字段时，返回{@code -1}。
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * <p>获得此Http请求的Content-Type中的MIME部分，即，<strong>不包含字符编码</strong>。</p>
     * <p>对于一些文本类型的MIME，如“text/plain”，浏览器可能会在其后加上字符编码的后缀，如“text/plain; charset=utf-8”。对于这种情况，此函数只会返回MIME部分，如“text/Plain”。若想获得字符编码，请使用{@link #getContentCharset()}；若想获得原始的Content-Type信息，请使用{@code getRequestInfo().headers.getFieldValue("Content-Type")}。</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #analyseContent()}函数后调用。</p>
     *
     * @return 此Http请求的Content-Type中的MIME部分。若此请求的请求头中未包含Content-Type，则返回{@code null}。
     *
     * @see #getContentCharset()
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * <p>获得此Http请求的Content-Type中的字符编码部分。</p>
     * <p>对于一些文本类型的MIME，如“text/plain”，浏览器可能会在其后加上字符编码的后缀，如“text/plain; charset=utf-8”。这种情况下可以使用此函数获得字符编码。若想获得字符编码，请使用{@link #getContentType()}；若想获得原始的Content-Type信息，请使用{@code getRequestInfo().headers.getFieldValue("Content-Type")}。</p>
     * <p></p>
     * <p>注意：此函数需要在成功调用{@link #analyseContent()}函数后调用。</p>
     *
     * @return 此Http请求的Content-Type中的字符编码部分。若此请求的请求头中未包含Content-Type，或此请求的Content-Type未包含字符编码信息，则返回{@code null}。
     *
     * @see #getContentType()
     */
    public Charset getContentCharset() {
        return contentCharset;
    }
}
