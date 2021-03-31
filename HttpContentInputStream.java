package com.davidsoft.serverprotect.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class HttpContentInputStream extends InputStream {

    public static final long CONTENT_LENGTH_UNKNOWN = -1;

    private final LengthLimitInputStream source;
    private final boolean chunked;
    private final Constructor<? extends InputStream> decorateStreamConstructor;

    private InputStream decoratedSource;

    private int peeked;
    private boolean reachedEnd;

    private int readingState;

    public HttpContentInputStream(InputStream source, boolean chunked, long contentLength, Class<? extends InputStream> decorateStreamClass) throws IOException {
        this.source = new LengthLimitInputStream(source);
        this.chunked = chunked;

        if (decorateStreamClass == null) {
            decorateStreamConstructor = null;
        }
        else {
            try {
                decorateStreamConstructor = decorateStreamClass.getDeclaredConstructor(InputStream.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(decorateStreamClass.toString() + "不是一个装饰输入流。", e);
            }
        }
        if (chunked) {
            decoratedSource = this.source;
        }
        else {
            if (decorateStreamConstructor == null) {
                decoratedSource = this.source;
            }
            else {
                try {
                    decoratedSource = decorateStreamConstructor.newInstance(this.source);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new IOException(e);
                }
            }
            if (contentLength != CONTENT_LENGTH_UNKNOWN) {
                this.source.setLimit(contentLength);
            }
            peeked = decoratedSource.read();
            reachedEnd = (peeked == -1);
        }
    }

    public boolean reachedEnd() {
        return reachedEnd;
    }

    public int read() throws IOException {
        if (reachedEnd) {
            return -1;
        }
        if (chunked) {
            return readChunked();
        }
        return readToEnd();
    }

    private int readToEnd() throws IOException {
        int ret = peeked;
        peeked = decoratedSource.read();
        reachedEnd = (peeked == -1);
        return ret;
    }

    private int readChunked() throws IOException {
        long chunkLength = 0;
        while (true) {
            int b;
            if (readingState == 5) {
                b = decoratedSource.read();
                if (b != -1) {
                    return b;
                }
                readingState = 0;
                source.cancelLimit();
            }
            b = source.read();
            if (b == -1) {
                throw new EOFException("Unexpected EOF in chunked transferring.");
            }
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
                        default:
                            throw new IOException("Malformed chunked transferring.");
                    }
                    break;
                case 4:
                    //读取chunk大小结束，等待换行符时，遇到\r的状态
                    if (b == '\n') {
                        //至此chunk大小接收成功
                        if (chunkLength == 0) {
                            reachedEnd = true;
                        }
                        else {
                            source.setLimit(chunkLength);
                            try {
                                decoratedSource = decorateStreamConstructor.newInstance(source);
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                throw new IOException(e);
                            }
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

    public void close() throws IOException {
        source.close();
    }
}
