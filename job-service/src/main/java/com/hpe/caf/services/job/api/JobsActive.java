package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsActive {

    private static final Logger LOG = LoggerFactory.getLogger(JobsActive.class);

    public static class JobsActiveResult {
        public final boolean active;
        public final int statusCheckIntervalSecs;

        public JobsActiveResult(boolean active, int statusCheckIntervalSecs) {
            this.active = active;
            this.statusCheckIntervalSecs = statusCheckIntervalSecs;
        }
    }

    public static JobsActiveResult isJobActive(String jobId) throws Exception {
        boolean active;
        int statusCheckIntervalMillis;

        try {
            LOG.info("isJobActive: Starting...");

            //  Make sure the job id has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(jobId)) {
                LOG.error("isJobActive: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
            }

            //  Make sure the job id does not contain any invalid characters.
            if (ApiServiceUtil.containsInvalidCharacters(jobId)) {
                LOG.error("isJobActive: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
            }

            //  Get app config settings.
            LOG.debug("isJobActive: Reading database connection properties...");
            AppConfig config = ApiServiceUtil.getAppConfigProperties();

            //  Get the number of seconds after which it is appropriate to try to confirm that the task has not been cancelled or aborted.
            statusCheckIntervalMillis = Integer.parseInt(config.getStatusCheckTime());

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Check if the specified job is active or not.
            LOG.info("isJobActive: Checking job status...");
            active = databaseHelper.isJobActive(jobId);

        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }

        LOG.info("isJobActive: Done.");
        return new JobsActiveResult(active,statusCheckIntervalMillis);
    }

}
