package com.davidsoft.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpResponseInfo {

    public String protocolVersion;
    public int responseCode;
    public String responseDescription;
    public final HttpHeaders headers;

    public HttpResponseInfo() {
        headers = new HttpHeaders();
    }

    public HttpResponseInfo(int responseCode) {
        this("HTTP/1.1", responseCode, getDefaultResponseDescription(responseCode), new HttpHeaders());
    }

    public HttpResponseInfo(String protocolVersion, int responseCode, String responseDescription) {
        this(protocolVersion, responseCode, responseDescription, new HttpHeaders());
    }

    private HttpResponseInfo(String protocolVersion, int responseCode, String responseDescription, HttpHeaders headers) {
        this.protocolVersion = protocolVersion;
        this.responseCode = responseCode;
        this.responseDescription = responseDescription;
        this.headers = headers;
    }

    //此方法不会写入多余的空行
    public void toHttpStream(OutputStream out) throws IOException {
        out.write((protocolVersion + " " + responseCode + " " + responseDescription + "\r\n").getBytes(StandardCharsets.UTF_8));
        headers.toResponseStream(out);
    }

    //此方法会接收多余的空行
    public static HttpResponseInfo fromHttpStream(InputStream in) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line = Utils.readHttpLine(in, stringBuilder);
        //默认服务器不会发来不可解析的信息
        int findPos = line.indexOf(' ');
        String protocolVersion = line.substring(0, findPos);
        findPos++;
        int findEnd = line.indexOf(' ', findPos);
        int responseCode = Integer.parseInt(line.substring(findPos, findEnd));
        String responseDescription = line.substring(findEnd + 1);
        HttpHeaders headers = HttpHeaders.fromResponseStream(in, stringBuilder);
        if (headers == null) {
            return null;
        }
        return new HttpResponseInfo(protocolVersion, responseCode, responseDescription, headers);
    }

    public static String getDefaultResponseDescription(int responseCode) {
        switch (responseCode) {
            case 100:
                return "Continue";
            case 101:
                return "Switching Protocols";
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 202:
                return "Accepted";
            case 203:
                return "Non-Authoritative Information";
            case 204:
                return "No Content";
            case 205:
                return "Reset Content";
            case 206:
                return "Partial Content";
            case 300:
                return "Multiple Choices";
            case 301:
                return "Moved Permanently";
            case 302:
                return "Found";
            case 303:
                return "See Other";
            case 304:
                return "Not Modified";
            case 305:
                return "Use Proxy";
            case 307:
                return "Temporary Redirect";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 402:
                return "Payment Required";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 406:
                return "Not Acceptable";
            case 407:
                return "Proxy Authentication Required";
            case 408:
                return "Request Time-out";
            case 409:
                return "Conflict";
            case 410:
                return "Gone";
            case 411:
                return "Length Required";
            case 412:
                return "Precondition Failed";
            case 413:
                return "Request Entity Too Large";
            case 414:
                return "Request-URI Too Large";
            case 415:
                return "Unsupported Media Type";
            case 416:
                return "Requested range not satisfiable";
            case 417:
                return "Expectation Failed";
            case 500:
                return "Internal Server Error";
            case 501:
                return "Not Implemented";
            case 502:
                return "Bad Gateway";
            case 503:
                return "Service Unavailable";
            case 504:
                return "Gateway Time-out";
            case 505:
                return "HTTP Version not supported";
            default:
                return null;
        }
    }

    /*

    public static HttpResponseInfo build233() {
        HttpResponseInfo info = buildCommon(true);
        info.responseCode = 233;
        info.responseDescription = "You Are Detected";
        return info;
    }

    public static HttpResponseInfo build400() {
        HttpResponseInfo info = buildCommon(true);
        info.responseCode = 400;
        info.responseDescription = "Bad Request";
        return info;
    }

    public static HttpResponseInfo build403() {
        HttpResponseInfo info = buildCommon(true);
        info.responseCode = 403;
        info.responseDescription = "Forbidden";
        return info;
    }

    public static HttpResponseInfo build411() {
        HttpResponseInfo info = buildCommon(true);
        info.responseCode = 411;
        info.responseDescription = "Length Required";
        return info;
    }

    public static HttpResponseInfo build500() {
        HttpResponseInfo info = buildCommon(true);
        info.responseCode = 500;
        info.responseDescription = "Internal Server Error";
        return info;
    }

    public static HttpResponseInfo build503() {
        HttpResponseInfo info = buildCommon(true);
        info.responseCode = 503;
        info.responseDescription = "Service Unavailable";
        return info;
    }

    public static HttpResponseInfo buildCommon(boolean closeConnection) {
        HttpResponseInfo info = new HttpResponseInfo();
        info.headers.put("server", new String[]{"Server", "DSSP_PRO/1.0"});
        if (closeConnection) {
            info.setHeaderField("Connection", "close");
        }
        else {
            info.setHeaderField("Connection", "keep-alive");
        }
        return info;
    }
     */
}
