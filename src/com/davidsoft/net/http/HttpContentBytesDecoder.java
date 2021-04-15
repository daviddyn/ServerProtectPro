package com.davidsoft.net.http;

import java.io.IOException;
import java.io.InputStream;

public class HttpContentBytesDecoder implements HttpContentDecoder<byte[]> {

    @Override
    public byte[] onDecode(InputStream in, long contentLength) throws UnacceptableException, IOException {
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
