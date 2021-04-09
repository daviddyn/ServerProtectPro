package com.davidsoft.http;

import com.davidsoft.collections.ReadOnlySet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

public final class HttpContentEncodedStreamFactory {

    private static final ReadOnlySet<String> supportedContentEncodings = new ReadOnlySet<>(new HashSet<>(Arrays.asList(null, "gzip", "deflate")));

    public static InputStream instanceInputStream(InputStream source, String contentEncoding) throws IOException {
        if (contentEncoding == null) {
            return source;
        }
        switch (contentEncoding.toLowerCase()) {
            case "gzip":
                return new GZIPInputStream(source);
            case "deflate":
                return new DeflaterInputStream(source);
            default:
                return null;
        }
    }

    public static EncodingOutputStream instanceOutputStream(OutputStream source, String contentEncoding) throws IOException {
        if (contentEncoding == null) {
            return new EncodingOutputStream(source);
        }
        switch (contentEncoding.toLowerCase()) {
            case "gzip":
                return new GZipEncodingOutputStream(source);
            case "deflate":
                return new DeflateEncodingOutputStream(source);
            default:
                return null;
        }
    }

    public static Collection<String> getSupportedContentEncodings() {
        return supportedContentEncodings;
    }
}
