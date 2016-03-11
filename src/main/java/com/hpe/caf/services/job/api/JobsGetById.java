package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsGetById {

    private static final Logger LOG = LoggerFactory.getLogger(JobsGetById.class);

    public static Job getJob(String jobId) throws Exception {

        Job job;

        try {
            LOG.info("getJobById: Starting...");

            //  Make sure the job id has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(jobId)) {
                LOG.error("getJobById: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
            }

            //  Make sure the job id does not contain any invalid characters.
            if (ApiServiceUtil.containsInvalidCharacters(jobId)) {
                LOG.error("getJobById: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
            }

            //  Get app config settings.
            LOG.debug("getJobById: Reading database connection properties...");
            AppConfig config = ApiServiceUtil.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Get the job definition for the specified job id.
            LOG.info("getJobById: Getting job definition...");
            job = databaseHelper.getJob(jobId);

            LOG.info("getJobById: Done.");
            return job;

        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }
    }
}
