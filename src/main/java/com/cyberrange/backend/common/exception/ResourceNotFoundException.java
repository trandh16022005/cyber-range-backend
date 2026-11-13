package com.cyberrange.backend.common.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates an exception with a descriptive error message.
     *
     * @param message description of the missing resource
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
