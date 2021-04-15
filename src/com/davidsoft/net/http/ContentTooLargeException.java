package com.davidsoft.net.http;

public class ContentTooLargeException extends UnacceptableException {

    public ContentTooLargeException() {
        super();
    }

    public ContentTooLargeException(String message) {
        super(message);
    }

    public ContentTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentTooLargeException(Throwable cause) {
        super(cause);
    }
}
