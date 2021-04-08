package com.davidsoft.serverprotect.http;
import java.nio.charset.*;
import java.io.*;

public class HttpContentForwardProvider implements HttpContentProvider {

    private final HttpRequestReceiver requestReceiver;
    
    public HttpContentForwardProvider(HttpRequestReceiver requestReceiver) {
        this.requestReceiver = requestReceiver;
    }
    
    @Override
    public String getMimeType() {
        return requestReceiver.getContentType();
    }

    @Override
    public Charset getCharset() {
        return requestReceiver.getContentCharset();
    }

    @Override
    public long getContentLength() {
        return requestReceiver.getContentLength();
    }

    @Override
    public boolean useChunkedTransfer() {
        return requestReceiver.hasSuspendedHeaders();
    }

    @Override
    public void onProvide(OutputStream out) throws IOException {
        requestReceiver.getContentRawInputStream().transferTo(out);
        if (requestReceiver.hasSuspendedHeaders()) {
            requestReceiver.readSuspendedHeaders();
        }
    }

    @Override
    public HttpHeaders getSuspendedHeader() {
        if (requestReceiver.hasSuspendedHeaders()) {
            return requestReceiver.getSuspendedHeaders();
        }
        else {
            return null;
        }
    }
}
