package com.hpe.caf.worker.jobtracking;

/**
 * Thrown when a failure occurs in connecting or reporting to a Job Database.
 */
public class JobReportingException extends Exception {
    public JobReportingException() {
        super();
    }


    /**
     * Create a JobReportingException
     * @param message information explaining the exception
     */
    public JobReportingException(String message) {
        super(message);
    }


    /**
     * Create a JobReportingException
     * @param message information explaining the exception
     * @param cause the original cause of this exception
     */
    public JobReportingException(String message, Throwable cause) {
        super(message, cause);
    }
}
