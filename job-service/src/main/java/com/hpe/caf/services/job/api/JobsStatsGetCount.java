package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.configuration.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsStatsGetCount {

    private static final Logger LOG = LoggerFactory.getLogger(JobsStatsGetCount.class);

    public static long getJobsCount(final String jobId, final String statusType) throws Exception {

        long jobsCount;

        try {
            LOG.info("getJobsCount: Starting...");

            //  Get app config settings.
            LOG.debug("getJobsCount: Reading database connection properties...");
            AppConfig config = ApiServiceUtil.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Get number of job definitions in the system.
            LOG.info("getJobsCount: Getting number of job definitions...");
            jobsCount = databaseHelper.getJobsCount(jobId, statusType);
        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }

        LOG.info("getJobsCount: Done.");
        return jobsCount;
    }
}
