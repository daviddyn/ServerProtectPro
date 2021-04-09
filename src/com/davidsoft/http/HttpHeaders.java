package com.davidsoft.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpHeaders {

    public final LinkedHashMap<String, String[]> headers;  //key只存小写，且不包含cookie
    public final LinkedHashMap<String, String> cookies;   //key未经url编码；value在request时存储未经url编码的值，在response时存储原始行(经url编码)

    public HttpHeaders() {
        headers = new LinkedHashMap<>();
        cookies = new LinkedHashMap<>();
    }

    public HttpHeaders(HttpHeaders cloneFrom) {
        headers = new LinkedHashMap<>(cloneFrom.headers);
        cookies = new LinkedHashMap<>(cloneFrom.cookies);
    }

    public boolean containsField(String fieldName) {
        return headers.containsKey(fieldName.toLowerCase());
    }

    public String getFieldValue(String fieldName) {
        String[] fieldValue = headers.get(fieldName.toLowerCase());
        if (fieldValue == null) {
            return null;
        }
        return fieldValue[1];
    }

    public void setFieldValue(String fieldName, String value) {
        String[] pair = headers.get(fieldName.toLowerCase());
        if (pair == null) {
            pair = new String[]{fieldName, value};
            headers.put(fieldName.toLowerCase(), pair);
        }
        else {
            pair[1] = value;
        }
    }

    public void removeField(String fieldName) {
        headers.remove(fieldName.toLowerCase());
    }

    public void mergeFields(HttpHeaders another) {
        headers.putAll(another.headers);
        cookies.putAll(another.cookies);
    }

    private String toRequestCookieLine() {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (entry.getKey() != null) {
                builder.append(UrlCodec.urlEncode(entry.getKey().getBytes(StandardCharsets.UTF_8))).append('=');
            }
            builder.append(UrlCodec.urlEncode(entry.getValue().getBytes(StandardCharsets.UTF_8)));
            if (i < cookies.size() - 1) {
                builder.append("; ");
            }
            i++;
        }
        return builder.toString();
    }

    //此方法不会写入多余的空行
    public void toRequestStream(OutputStream out) throws IOException {
        for (String[] head : headers.values()) {
            out.write((head[0] + ": " + head[1] + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        if (!cookies.isEmpty()) {
            out.write(("Cookie: " + toRequestCookieLine() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    //此方法不会写入多余的空行
    public void toResponseStream(OutputStream out) throws IOException {
        for (String[] head : headers.values()) {
            out.write((head[0] + ": " + head[1] + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        for (String cookie : cookies.values()) {
            out.write(("Set-Cookie: " + cookie + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void parseRequestCookies(String cookieLine, Map<String, String> out) {
        for (String item : cookieLine.split("; ")) {
            String key;
            int findPos = item.indexOf('=');
            if (findPos == -1) {
                key = null;
            }
            else {
                key = UrlCodec.urlDecodeString(item.substring(0, findPos), StandardCharsets.UTF_8);
            }
            out.put(key, UrlCodec.urlDecodeString(item.substring(findPos + 1), StandardCharsets.UTF_8));
        }
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

    public int fromRequestStreamLimited(InputStream in, StringBuilder bufferReuse, int maxHeaderSize) throws IOException {
        String line;
        bufferReuse.delete(0, bufferReuse.length());
        int size = 0;
        //解析其余字段
        while (!(line = Utils.readHttpLine(in, bufferReuse)).isEmpty()) {
            int findPos = line.indexOf(": ");
            if (findPos == -1 || findPos == 0 || findPos > 32) {
                return INVALID_DATA;
            }
            String key = line.substring(0, findPos);
            String indexedKey = key.toLowerCase();
            if (indexedKey.equals("cookie")) {
                parseRequestCookies(line.substring(findPos + 2), cookies);
            }
            else {
                headers.put(indexedKey, new String[] {key, line.substring(findPos + 2)});
            }
            size += line.length();
            if (size > maxHeaderSize) {
                return HEADER_SIZE_EXCEED;
            }
        }
        return SUCCESS;
    }

    //此方法会接收多余的空行
    public static HttpHeaders fromRequestStream(InputStream in, StringBuilder bufferReuse) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        if (headers.fromRequestStreamLimited(in, bufferReuse, Integer.MAX_VALUE) != SUCCESS) {
            return null;
        }
        return headers;
    }

    //此方法会接收多余的空行
    public static HttpHeaders fromResponseStream(InputStream in, StringBuilder bufferReuse) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        String line;
        bufferReuse.delete(0, bufferReuse.length());
        //解析其余字段
        while (!(line = Utils.readHttpLine(in, bufferReuse)).isEmpty()) {
            int findPos = line.indexOf(": ");
            if (findPos == -1 || findPos == 0 || findPos > 32) {
                return null;
            }
            String key = line.substring(0, findPos);
            String indexedKey = key.toLowerCase();
            if (indexedKey.equals("set-cookie")) {
                String cookieLine = line.substring(findPos + 2);
                findPos = cookieLine.indexOf('=');
                headers.cookies.put(findPos == -1 ? null : UrlCodec.urlDecodeString(cookieLine.substring(0, findPos), StandardCharsets.UTF_8), cookieLine);
            }
            else {
                headers.headers.put(indexedKey, new String[] {key, line.substring(findPos + 2)});
            }
        }
        return headers;
    }

    public String toRequestString() {
        StringBuilder builder = new StringBuilder();
        for (String[] head : headers.values()) {
            builder.append(head[0]).append(": ").append(head[1]).append(System.lineSeparator());
        }
        if (!cookies.isEmpty()) {
            builder.append("Cookie: ");
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                builder.append(UrlCodec.urlEncode(entry.getKey().getBytes(StandardCharsets.UTF_8))).append("=").append(UrlCodec.urlEncode(entry.getValue().getBytes(StandardCharsets.UTF_8))).append("; ");
            }
            builder.delete(builder.length() - 2, builder.length());
        }
        builder.append(System.lineSeparator());
        return builder.toString();
    }

    public String toResponseString() {
        StringBuilder builder = new StringBuilder();
        for (String[] head : headers.values()) {
            builder.append(head[0]).append(": ").append(head[1]).append(System.lineSeparator());
        }
        if (!cookies.isEmpty()) {
            for (String line : cookies.values()) {
                builder.append("Set-Cookie: ").append(line).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }
}
