package com.davidsoft.net.http;

import com.davidsoft.url.StringEscapeDecoder;
import com.davidsoft.url.StringEscapeEncoder;
import com.davidsoft.url.URI;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;

public class HttpURIUtils {

    private static final StringEscapeEncoder httpUrlEncoder = src -> UrlCodec.urlEncode(src.getBytes(StandardCharsets.UTF_8));

    private static final StringEscapeDecoder httpUrlDecoder = src -> UrlCodec.urlDecodeString(src, StandardCharsets.UTF_8);

    public static String toLocationString(URI uri) {
        return uri.toLocationString(com.davidsoft.net.Utils.pathSeparator, httpUrlEncoder);
    }

    public static String toString(URI uri) {
        return uri.toString(com.davidsoft.net.Utils.pathSeparator, httpUrlEncoder);
    }

    public static URI parse(String source) throws ParseException {
        return URI.parse(source, com.davidsoft.net.Utils.pathSeparator, httpUrlDecoder);
    }
}
