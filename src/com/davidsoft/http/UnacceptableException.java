package com.davidsoft.http;

public class UnacceptableException extends Exception {

    public UnacceptableException() {
        super();
    }

    public UnacceptableException(String message) {
        super(message);
    }

    public UnacceptableException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnacceptableException(Throwable cause) {
        super(cause);
    }
}