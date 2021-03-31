package com.davidsoft.serverprotect.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class HttpContentForwardProvider implements HttpContentProvider {

    private final HttpRequestReceiver receiver;
    private final String mimeType;
    private final Charset contentCharset;
    private final int contentLength;
    private final boolean chunkedTransfer;

    public HttpContentForwardProvider(HttpRequestReceiver receiver) {
        this.receiver = receiver;
        String field = receiver.getRequestInfo().headers.getFieldValue("Content-Type");
        if (field == null) {
            mimeType = null;
            contentCharset = null;
        }
        else {
            int findPos = field.indexOf(";");
            if (findPos == -1) {
                mimeType = field;
                contentCharset = null;
            }
            else {
                mimeType = field.substring(0, findPos);
                contentCharset = Charset.forName(field.substring(findPos + 1).trim());
            }
        }
        field = receiver.getRequestInfo().headers.getFieldValue("Content-Length");
        int l;
        if (field == null) {
            l = -1;
        }
        else {
            try {
                l = Integer.parseInt(field);
            }
            catch (NumberFormatException e) {
                l = -1;
            }
        }
        contentLength = l;
        chunkedTransfer = "chunked".equals(receiver.getRequestInfo().headers.getFieldValue("Transfer-Encoding"));
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public Charset getCharset() {
        return contentCharset;
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
        new HttpContentTransferer(chunkedTransfer, contentLength).transfer(receiver.getRawInputStream(), out);
    }

    @Override
    public HttpHeaders getSuspendedHeader() {
        return null;
    }
}
