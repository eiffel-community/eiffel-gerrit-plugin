package com.ericsson.gerrit.plugins.eiffel.exceptions;

public class EiffelEventSenderException extends Exception {
    private static final long serialVersionUID = 1L;

    public EiffelEventSenderException(final String message, final Throwable e) {
        super(message, e);
    }

    public EiffelEventSenderException(final String message) {
        super(message);
    }
}
