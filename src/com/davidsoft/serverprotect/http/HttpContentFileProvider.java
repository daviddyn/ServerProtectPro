package com.davidsoft.serverprotect.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class HttpContentFileProvider implements HttpContentProvider {

    private final String mimeType;
    private final FileInputStream fileIn;
    private final Charset charset;
    private final int contentLength;
    private final boolean chunkedTransfer;

    public HttpContentFileProvider(File file, Charset charset) throws IOException {
        mimeType = Files.probeContentType(file.toPath());
        fileIn = new FileInputStream(file);
        this.charset = charset;
        if (file.length() > 1048576/*1MiB*/) {
            contentLength = 0;
            chunkedTransfer = true;
        }
        else {
            contentLength = (int) file.length();
            chunkedTransfer = false;
        }
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
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public boolean useChunkedTransfer() {
        return chunkedTransfer;
    }

    @Override
    public void onProvide(OutputStream out) throws IOException {
        fileIn.transferTo(out);
    }

    @Override
    public HttpHeaders getSuspendedHeader() {
        return null;
    }
}
