package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class HttpContentStringReceiver implements HttpContentReceiver<String> {

    private final Charset charset;

    public HttpContentStringReceiver(Charset charset) {
        this.charset = charset;
    }

    @Override
    public String onReceive(InputStream in, long contentLength) throws UnacceptableException, IOException {
        return new String(new HttpContentBytesReceiver().onReceive(in, contentLength), charset);
    }
}