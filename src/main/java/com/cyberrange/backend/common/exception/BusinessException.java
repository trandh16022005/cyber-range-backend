
package com.cyberrange.backend.common.exception;

/**
 * Exception thrown when a business rule is violated.
 */
public class BusinessException extends RuntimeException {

    /**
     * Creates a business exception with a descriptive message.
     *
     * @param message description of the business rule violation
     */
    public BusinessException(String message) {
        super(message);
    }
}
