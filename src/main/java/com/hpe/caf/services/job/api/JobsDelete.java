package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsDelete {

    private static final Logger LOG = LoggerFactory.getLogger(JobsGet.class);

    public static void deleteJob(String jobId) throws Exception {
        try {
            LOG.info("deleteJob: Starting...");

            //  Make sure the job id has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(jobId)) {
                LOG.error("deleteJob: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
            }

            //  Make sure the job id does not contain any invalid characters.
            if (ApiServiceUtil.containsInvalidCharacters(jobId)) {
                LOG.error("deleteJob: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
            }

            //  Get app config settings.
            LOG.debug("deleteJob: Reading database connection properties...");
            AppConfig config = ApiServiceUtil.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Delete the specified job from the system.
            LOG.info("deleteJob: Deleting job...");
            databaseHelper.deleteJob(jobId);

            LOG.info("deleteJob: Done.");

        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }
    }

}
