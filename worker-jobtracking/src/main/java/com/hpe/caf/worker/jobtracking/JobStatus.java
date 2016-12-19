package com.hpe.caf.worker.jobtracking;

/**
 * Enumeration mirroring the job_status ENUM defined in the Job Database.
 * Refer to \CAFdev\job-service-db\src\main\resources\changelog.xml
 */
public enum JobStatus {
    Active,
    Cancelled,
    Completed,
    Failed,
    Paused,
    Waiting
}
