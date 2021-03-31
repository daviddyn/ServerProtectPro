package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public interface HttpContentProvider {

    String getMimeType();

    Charset getCharset();

    int getContentLength();

    boolean useChunkedTransfer();

    //值得注意的是，ChunkedTransfer由外部实现，这里尽管向outputStream里写就好~
    void onProvide(OutputStream out) throws IOException;

    HttpHeaders getSuspendedHeader();
}