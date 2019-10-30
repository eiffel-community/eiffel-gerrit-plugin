package com.ericsson.gerrit.plugins.eiffel.exceptions;

public class HttpRequestFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public HttpRequestFailedException(final String message, final Throwable e) {
        super(message, e);
    }

    public HttpRequestFailedException(final String message) {
        super(message);
    }

    public HttpRequestFailedException(final Throwable e) {
        super(e);
    }
}
