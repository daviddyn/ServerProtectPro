package com.davidsoft.net.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HttpContentTransferer {

    private final boolean chunked;
    private final long contentLength;

    private boolean reachedEnd;

    public HttpContentTransferer(boolean chunked, long contentLength) {
        this.chunked = chunked;
        this.contentLength = contentLength;
    }

    public void transfer(InputStream source, OutputStream dest) throws IOException {
        if (chunked) {
            transferChunk(source, dest);
        }
        else {
            if (contentLength == -1) {
                transferToEnd(source, dest);
            }
            else {
                transferToLength(source, dest);
            }
        }
    }

    private void transferToEnd(InputStream source, OutputStream dest) throws IOException {
        source.transferTo(dest);
    }

    private void transferToLength(InputStream source, OutputStream dest) throws IOException {
        new LengthLimitInputStream(source, contentLength).transferTo(dest);
    }

    private void transferChunk(InputStream source, OutputStream dest) throws IOException {
        long chunkLength = 0;
        int readingState = 0;
        LengthLimitInputStream lengthLimitedSource = new LengthLimitInputStream(source);
        while (true) {
            int b;
            if (readingState == 5) {
                b = lengthLimitedSource.read();
                if (b != -1) {
                    dest.write(b);
                    continue;
                }
                readingState = 0;
                lengthLimitedSource.cancelLimit();
            }
            b = lengthLimitedSource.read();
            if (b == -1) {
                throw new EOFException("Unexpected EOF in chunked transferring.");
            }
            dest.write(b);
            switch (readingState) {
                case 0:
                    //等待开始读取chunk大小状态
                    if ('0' <= b && b <= '9') {
                        chunkLength = b - '0';
                        readingState = 2;
                    }
                    else if ('A' <= b && b <= 'F') {
                        chunkLength = b + 10 - 'A';
                        readingState = 2;
                    }
                    else if ('a' <= b && b <= 'f') {
                        chunkLength = b + 10 - 'a';
                        readingState = 2;
                    }
                    else switch (b) {
                            case ' ':
                                break;
                            case '\r':
                                readingState = 1;
                                break;
                            default:
                                throw new IOException("Malformed chunked transferring.");
                        }
                    break;
                case 1:
                    //等待开始读取chunk大小时，遇到\r的状态
                    if (b == '\n') {
                        readingState = 0;
                    }
                    else {
                        throw new IOException("Malformed chunked transferring.");
                    }
                    break;
                case 2:
                    //读取chunk大小状态
                    if ('0' <= b && b <= '9') {
                        chunkLength = ((chunkLength << 4) | (b - '0'));
                    }
                    else if ('A' <= b && b <= 'F') {
                        chunkLength = ((chunkLength << 4) | (b + 10 - 'A'));
                    }
                    else if ('a' <= b && b <= 'f') {
                        chunkLength = ((chunkLength << 4) | (b + 10 - 'a'));
                    }
                    else switch (b) {
                            case ' ':
                                readingState = 3;
                                break;
                            case '\r':
                                readingState = 4;
                                break;
                            default:
                                throw new IOException("Malformed chunked transferring.");
                        }
                    break;
                case 3:
                    //读取chunk大小结束，等待换行符的状态
                    switch (b) {
                        case ' ':
                            break;
                        case '\r':
                            readingState = 4;
                            break;
                        default:
                            throw new IOException("Malformed chunked transferring.");
                    }
                    break;
                case 4:
                    //读取chunk大小结束，等待换行符时，遇到\r的状态
                    if (b == '\n') {
                        //至此chunk大小接收成功
                        if (chunkLength == 0) {
                            return;
                        }
                        else {
                            lengthLimitedSource.setLimit(chunkLength);
                            readingState = 5;
                        }
                    }
                    else {
                        throw new IOException("Malformed chunked transferring.");
                    }
                    break;
            }
        }
    }
}
