package com.ericsson.gerrit.plugins.eiffel.exceptions;

public class HttpRequestFailedException extends Exception {
    private static final long serialVersionUID = 1L;

    public HttpRequestFailedException(final String message, final Throwable e) {
        super(message, e);
    }

    public HttpRequestFailedException(final String message) {
        super(message);
    }
}
