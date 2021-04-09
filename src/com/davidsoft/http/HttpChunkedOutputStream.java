package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class HttpChunkedOutputStream extends OutputStream {

    private final OutputStream out;

    public HttpChunkedOutputStream(OutputStream out) {
        this.out = out;
    }

    public void finish() throws IOException {
        out.write("0\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public void write(int b) throws IOException {
        out.write("1\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(b);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public void write(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        out.write(String.format("%X\r\n", len).getBytes(StandardCharsets.UTF_8));
        out.write(b, off, len);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        finish();
        out.close();
    }
}
