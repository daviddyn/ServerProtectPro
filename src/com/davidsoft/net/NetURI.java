package com.davidsoft.net;

import com.davidsoft.net.http.UrlCodec;
import com.davidsoft.url.StringEscapeDecoder;
import com.davidsoft.url.StringEscapeEncoder;
import com.davidsoft.url.URI;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;

public class NetURI {

    private static final StringEscapeEncoder httpUrlEncoder = src -> UrlCodec.urlEncode(src.getBytes(StandardCharsets.UTF_8));

    private static final StringEscapeDecoder httpUrlDecoder = src -> UrlCodec.urlDecodeString(src, StandardCharsets.UTF_8);

    public static String toLocationString(URI uri) {
        return uri.toLocationString(Utils.pathSeparator, httpUrlEncoder);
    }

    public static String toString(URI uri) {
        return uri.toString(Utils.pathSeparator, httpUrlEncoder);
    }

    public static URI parse(String source) throws ParseException {
        return URI.parse(source, Utils.pathSeparator, httpUrlDecoder);
    }
}
