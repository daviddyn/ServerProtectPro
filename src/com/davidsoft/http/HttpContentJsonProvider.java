package com.davidsoft.serverprotect.http;

import com.davidsoft.utils.JsonNode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class HttpContentJsonProvider implements HttpContentProvider {

    private final HttpContentStringProvider stringProvider;

    public HttpContentJsonProvider(JsonNode jsonNode, Charset encodingCharset) {
        stringProvider = new HttpContentStringProvider(jsonNode.toString(), encodingCharset, "application/json", encodingCharset);
    }

    @Override
    public String getMimeType() {
        return stringProvider.getMimeType();
    }

    @Override
    public Charset getCharset() {
        return stringProvider.getCharset();
    }

    @Override
    public long getContentLength() {
        return stringProvider.getContentLength();
    }

    @Override
    public boolean useChunkedTransfer() {
        return stringProvider.useChunkedTransfer();
    }

    @Override
    public void onProvide(OutputStream out) throws IOException {
        stringProvider.onProvide(out);
    }

    @Override
    public HttpHeaders getSuspendedHeader() {
        return null;
    }
}
