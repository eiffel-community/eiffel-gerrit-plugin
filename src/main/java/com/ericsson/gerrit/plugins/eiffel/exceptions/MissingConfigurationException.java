package com.ericsson.gerrit.plugins.eiffel.exceptions;

public class MissingConfigurationException extends Exception {
    private static final long serialVersionUID = 1L;

    public MissingConfigurationException(final String message, final Throwable e) {
        super(message, e);
    }

    public MissingConfigurationException(final String message) {
        super(message);
    }
}