package com.davidsoft.serverprotect.http;
import java.nio.charset.*;
import java.io.*;

public class HttpContentForwardProvider implements HttpContentProvider {

    private final HttpContentReceiver contentReceiver;
    
    public HttpContentForwardProvider(HttpContentReceiver contentReceiver) {
        this.contentReceiver = contentReceiver;
    }
    
    @Override
    public String getMimeType() {
        return contentReceiver.getContentType();
    }

    @Override
    public Charset getCharset() {
        return contentReceiver.getContentCharset();
    }

    @Override
    public long getContentLength() {
        return contentReceiver.getContentLength();
    }

    @Override
    public boolean useChunkedTransfer() {
        return contentReceiver.hasSuspendedHeaders();
    }

    @Override
    public void onProvide(OutputStream out) throws IOException {
        contentReceiver.getContentRawInputStream().transferTo(out);
        if (contentReceiver.hasSuspendedHeaders()) {
            contentReceiver.receiveSuspendedHeaders();
        }
    }

    @Override
    public HttpHeaders getSuspendedHeader() {
        if (contentReceiver.hasSuspendedHeaders()) {
            return contentReceiver.getSuspendedHeaders();
        }
        else {
            return null;
        }
    }
}
