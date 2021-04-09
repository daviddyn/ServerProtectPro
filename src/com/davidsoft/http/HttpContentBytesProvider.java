package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class HttpContentBytesProvider implements HttpContentProvider {

    private final byte[] data;
    private final String mimeType;
    private final Charset charset;

    public HttpContentBytesProvider(byte[] data, String mimeType, Charset charset) {
        this.data = data;
        this.mimeType = mimeType;
        this.charset = charset;
    }


    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public long getContentLength() {
        return data.length;
    }

    @Override
    public boolean useChunkedTransfer() {
        return false;
    }

    @Override
    public void onProvide(OutputStream out) throws IOException {
        out.write(data);
    }

    @Override
    public HttpHeaders getSuspendedHeader() {
        return null;
    }
}
