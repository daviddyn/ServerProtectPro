package com.davidsoft.net.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class LengthLimitInputStream extends InputStream {

    private final InputStream source;

    private long count;
    private long limit;

    public LengthLimitInputStream(InputStream source, long limit) {
        this.source = source;
        this.limit = limit;
    }

    public LengthLimitInputStream(InputStream source) {
        this(source, Long.MAX_VALUE);
    }

    public void setLimit(long length) {
        count = 0;
        limit = length;
    }

    public void cancelLimit() {
        setLimit(Long.MAX_VALUE);
    }

    public int read() throws IOException {
        if (count == limit) {
            return -1;
        }
        int ret = source.read();
        if (ret >= 0) {
            count++;
        }
        return ret;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        if (count + len > limit) {
            len = (int) (limit - count);
            if (len == 0) {
                return -1;
            }
        }
        len = source.read(b, off, len);
        if (len >= 0) {
            count += len;
        }
        return len;
    }

    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        if (count + n > limit) {
            n = limit - count;
        }
        n = source.skip(n);
        count += n;
        return n;
    }

    public void skipNBytes(long n) throws IOException {
        if (n >= 0 && count + n > limit) {
            throw new EOFException();
        }
        source.skipNBytes(n);
        count += n;
    }

    public int available() throws IOException {
        int sourceAvailable = source.available();
        if (count + sourceAvailable > limit) {
            sourceAvailable = (int) (limit - count);
        }
        return sourceAvailable;
    }

    public void close() throws IOException {
        source.close();
    }
}
