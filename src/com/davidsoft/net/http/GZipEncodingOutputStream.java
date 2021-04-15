package com.davidsoft.net.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class GZipEncodingOutputStream extends EncodingOutputStream {

    public GZipEncodingOutputStream(OutputStream source) throws IOException {
        super(new GZIPOutputStream(source));
    }

    public void finish() throws IOException {
        ((GZIPOutputStream) source).finish();
    }
}
