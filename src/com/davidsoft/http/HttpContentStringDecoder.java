package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class HttpContentStringDecoder implements HttpContentDecoder<String> {

    private final Charset charset;

    public HttpContentStringDecoder(Charset charset) {
        this.charset = charset;
    }

    @Override
    public String onDecode(InputStream in, long contentLength) throws UnacceptableException, IOException {
        return new String(new HttpContentBytesDecoder().onDecode(in, contentLength), charset);
    }
}