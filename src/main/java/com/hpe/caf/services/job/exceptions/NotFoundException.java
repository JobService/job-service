package com.hpe.caf.services.job.exceptions;

/**
 * Custom exception implemented for the job service api. Exceptions of this type map
 * directly onto http 404 status codes.
 */
public final class NotFoundException  extends Exception {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
