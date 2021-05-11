package com.davidsoft.net.http;

/**
 * 与{@link HttpContentInputStream}的区别是，此类仅根据chunked和contentLength分析读取何时结束，并不会对读取到的内容进行解码，从此流中读到的数据是原始数据。
 */
import java.io.*;

public class HttpContentRawInputStream extends InputStream {
    
    public static final long CONTENT_LENGTH_UNKNOWN = -1;

    private final LengthLimitInputStream source;
    private final boolean chunked;

    private int peeked;
    private boolean reachedEnd;

    private int readingState;

    private long chunkLength;

    /**
     * chunked = true, contentLength参数将被忽略。
     * chunked = false, contentLength=-1 代表直接读到流末尾(此时连接一定是不复用的)。
     * chunked = false, contentLength=具体数值 代表读取指定字节数。
     */
    public HttpContentRawInputStream(InputStream source, boolean chunked, long contentLength) throws IOException {
        this.source = new LengthLimitInputStream(source);
        this.chunked = chunked;
        if (chunked) {
            reachedEnd = false;
        }
        else {
            if (contentLength != CONTENT_LENGTH_UNKNOWN) {
                this.source.setLimit(contentLength);
            }
            peeked = this.source.read();
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
        peeked = source.read();
        reachedEnd = (peeked == -1);
        return ret;
    }

    private int readChunked() throws IOException {
        int b;
        if (readingState == 5) {
            b = source.read();
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
                        reachedEnd = true;
                    }
                    else {
                        source.setLimit(chunkLength);
                        readingState = 5;
                    }
                }
                else {
                    throw new IOException("Malformed chunked transferring.");
                }
                break;
        }
        return b;
    }

    public void close() throws IOException {
        source.close();
    }
}
