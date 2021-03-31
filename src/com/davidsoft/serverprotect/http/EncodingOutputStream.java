package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.OutputStream;

public class EncodingOutputStream extends OutputStream {

    protected final OutputStream source;

    public EncodingOutputStream(OutputStream source) {
        this.source = source;
    }

    public void write(int b) throws IOException {
        source.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        source.write(b, off, len);
    }

    public void finish() throws IOException {

    }

    public void flush() throws IOException {
        source.flush();
    }

    public void close() throws IOException {
        finish();
        source.close();
    }
}
