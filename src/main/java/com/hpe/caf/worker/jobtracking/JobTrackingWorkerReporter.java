package com.hpe.caf.worker.jobtracking;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.text.MessageFormat;

public class JobTrackingWorkerReporter {
    /**
     * The connection info for connecting to the Job Database.
     */
    @NotNull
    @Size(min = 1)
    private String jobDatabaseConnectionString;


    public JobTrackingWorkerReporter(String jobDatabaseConnectionString) {
        this.jobDatabaseConnectionString = jobDatabaseConnectionString;
    }


    public void reportJobTaskProgress(final String jobTaskId, final int estimatedPercentageCompleted) throws JobReportingException {
        try {
            //TODO
        } catch (Exception e) {
            throw new JobReportingException(MessageFormat.format("Failed to report job task progress for job task {0} : ", jobTaskId), e);
        }
    }


    public void reportJobTaskComplete(final String jobTaskId) throws JobReportingException {
        try {
            //TODO
        } catch (Exception e) {
            throw new JobReportingException(MessageFormat.format("Failed to report job task completion for job task {0} : ", jobTaskId), e);
        }
    }


    public void reportJobTaskFailure(final String jobTaskId, final int retries) throws JobReportingException {
        try {
            //TODO
        } catch (Exception e) {
            throw new JobReportingException(MessageFormat.format("Failed to report job task failure for job task {0} : ", jobTaskId), e);
        }
    }


    public void reportJobTaskRejected(final String jobTaskId, final int retries) throws JobReportingException {
        try {
            //TODO
        } catch (Exception e) {
            throw new JobReportingException(MessageFormat.format("Failed to report job task rejection for job task {0} : ", jobTaskId), e);
        }
    }


    /**
     * Try to connect to the Job Database using the connection info provided in ctor.
     */
    public boolean performHealthCheck() {
        //TODO
        /*
        try (xxx) {
            return bbb;
        } catch (Exception e) {
            return false;
        }
        */
        return true;
    }
}
