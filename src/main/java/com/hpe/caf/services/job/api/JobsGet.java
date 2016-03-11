package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.configuration.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsGet {

    private static final Logger LOG = LoggerFactory.getLogger(JobsGet.class);

    public static Job[] getJobs() throws Exception {

        Job[] jobs;

        try {
            LOG.info("getJobs: Starting...");

            //  Get app config settings.
            LOG.debug("getJobs: Reading database connection properties...");
            AppConfig config = ApiServiceUtil.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Get list of job definitions in the system.
            LOG.info("getJobs: Getting list of job definitions...");
            jobs = databaseHelper.getJobs();
        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }

        LOG.info("getJobs: Done.");
        return jobs;
    }
}
