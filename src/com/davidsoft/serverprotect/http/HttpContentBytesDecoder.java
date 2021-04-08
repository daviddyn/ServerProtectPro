package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.InputStream;

public class HttpContentBytesReceiver implements HttpContentReceiver<byte[]> {

    @Override
    public byte[] onReceive(InputStream in, long contentLength) throws UnacceptableException, IOException {
        if (contentLength == -1) {
            try {
                return in.readAllBytes();
            }
            catch (OutOfMemoryError e) {
                e.printStackTrace();
                throw new ContentTooLargeException();
            }
        }
        else {
            if (contentLength > DirectReferByteArrayOutputStream.MAX_ARRAY_LENGTH) {
                throw new ContentTooLargeException();
            }
            try {
                return in.readNBytes((int) contentLength);
            }
            catch (OutOfMemoryError e) {
                e.printStackTrace();
                throw new ContentTooLargeException();
            }
        }
    }
}
