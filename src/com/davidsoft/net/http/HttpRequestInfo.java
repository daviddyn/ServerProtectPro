package com.davidsoft.net.http;

import com.davidsoft.net.NetURI;
import com.davidsoft.url.URI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

public final class HttpRequestInfo {

    public String method;
    public URI uri;
    public String pathParameters;   //原始未经解析的请求参数，经过了url编码，如果url以?结尾，则此字段为空串，如果不包含参数，则此字段为null。
    public String protocolVersion;
    public final HttpHeaders headers;

    public HttpRequestInfo() {
        headers = new HttpHeaders();
    }

    public HttpRequestInfo(HttpRequestInfo cloneFrom) {
        method = cloneFrom.method;
        uri = cloneFrom.uri;
        pathParameters = cloneFrom.pathParameters;
        protocolVersion = cloneFrom.protocolVersion;
        headers = new HttpHeaders(cloneFrom.headers);
    }

    public HttpRequestInfo(String method, URI uri, String pathParameters, String protocolVersion) {
        this.method = method;
        this.uri = uri;
        this.pathParameters = pathParameters;
        this.protocolVersion = protocolVersion;
        this.headers = new HttpHeaders();
    }

    public HttpRequestInfo(String method, URI uri, String pathParameters, String protocolVersion, HttpHeaders headers) {
        this.method = method;
        this.uri = uri;
        this.pathParameters = pathParameters;
        this.protocolVersion = protocolVersion;
        this.headers = headers;
    }

    //此方法不会写入多余的空行
    public void toHttpStream(OutputStream out) throws IOException {
        out.write((method + " " + NetURI.toString(uri) + (pathParameters == null ? "" : "?" + pathParameters) + " HTTP/" + protocolVersion + "\r\n").getBytes(StandardCharsets.UTF_8));
        headers.toRequestStream(out);
    }

    /**
     * 从输入流中成功解析Http请求头。
     */
    public static final int SUCCESS = 0;
    /**
     * 输入流中Http请求头语法不正确。
     */
    public static final int INVALID_DATA = 1;
    /**
     * 从输入流中读入的Http请求头超过了给定的字节数。
     */
    public static final int HEADER_SIZE_EXCEED = 2;
    /**
     * 从输入流中读入的Http请求头中的URI部分超过了给定的字符数。
     */
    public static final int PATH_LENGTH_EXCEED = 3;

    //此方法会接收多余的空行
    public int fromHttpStream(InputStream in, int maxPathLength, int maxHeaderSize)throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        //读第一行
        String line = Utils.readHttpLine(in, stringBuilder);
        //解析method
        int findPos = line.indexOf(' ');
        if (findPos == -1 || findPos == 0 || findPos > 7) {
            return INVALID_DATA;
        }
        method = line.substring(0, findPos);
        findPos++;

        //解析uri
        int findEnd = line.indexOf(' ', findPos);
        if (findEnd == -1 || findEnd - findPos == 0) {
            return INVALID_DATA;
        }
        if (findEnd - findPos > maxPathLength) {
            return PATH_LENGTH_EXCEED;
        }
        String rawPath = line.substring(findPos, findEnd);
        int parameterSep = rawPath.indexOf("?");
        String uri;
        if (parameterSep == -1) {
            uri = rawPath;
            pathParameters = null;
        }
        else {
            uri = rawPath.substring(0, parameterSep);
            pathParameters = rawPath.substring(parameterSep + 1);
        }
        try {
            this.uri = NetURI.parse(uri);
        } catch (ParseException e) {
            e.printStackTrace();
            return INVALID_DATA;
        }
        findPos = findEnd + 1;

        //解析protocolVersion
        if (findPos == line.length()) {
            return INVALID_DATA;
        }
        if (!line.startsWith("HTTP/", findPos)) {
            return INVALID_DATA;
        }
        protocolVersion = line.substring(findPos + 5);
        try {
            Float.parseFloat(protocolVersion);
        }
        catch (NumberFormatException e) {
            return INVALID_DATA;
        }
        switch (headers.fromRequestStreamLimited(in, stringBuilder, maxHeaderSize)) {
            case HttpHeaders.INVALID_DATA:
                return INVALID_DATA;
            case HttpHeaders.HEADER_SIZE_EXCEED:
                return HEADER_SIZE_EXCEED;
            default:
                return SUCCESS;
        }
    }

    @Override
    public String toString() {
        return method + " " + NetURI.toString(uri) + (pathParameters == null ? "" : "?" + pathParameters ) + " HTTP/" + protocolVersion + System.lineSeparator() + headers.toRequestString();
    }
}