package com.davidsoft.http;

import java.io.IOException;
import java.io.InputStream;

public interface HttpContentDecoder<T> {

    T onDecode(InputStream in, long contentLength) throws UnacceptableException, IOException;
}