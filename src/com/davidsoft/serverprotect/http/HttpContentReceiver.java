package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.InputStream;

public interface HttpContentReceiver<T> {

    T onReceive(InputStream in, long contentLength) throws UnacceptableException, IOException;
}