package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check for the JobTrackingWorker.
 */
public class JobTrackingWorkerHealthCheck implements HealthReporter {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorker.class);

    private final JobTrackingWorkerReporter reporter;


    public JobTrackingWorkerHealthCheck(JobTrackingWorkerReporter reporter) {
        this.reporter = reporter;
    }


    /**
     * The health check checks if all the external components that the worker depends on are available.
     */
    @Override
    public HealthResult healthCheck() {
        try
        {
            if (!reporter.performHealthCheck()) {
                LOG.warn("Error contacting Job Database.");
                return new HealthResult(HealthStatus.UNHEALTHY, "Job Database connection check failed.");
            }
            return HealthResult.RESULT_HEALTHY;
        } catch (Exception e) {
            LOG.warn("Error contacting Job Database. ", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Job Database connection check failed. " + e.getMessage());
        }
    }
}
