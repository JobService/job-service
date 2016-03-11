package com.hpe.caf.services.job.exceptions;

/**
 * Custom exception implemented for the job service api. Exceptions of this type map
 * directly onto http 400 status codes.
 */
public final class BadRequestException extends Exception {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
