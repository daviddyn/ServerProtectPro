package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

public class DeflateEncodingOutputStream extends EncodingOutputStream {

    public DeflateEncodingOutputStream(OutputStream source) {
        super(new DeflaterOutputStream(source));
    }

    public void finish() throws IOException {
        ((DeflaterOutputStream) source).finish();
    }
}
