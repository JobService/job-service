/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.configuration.AppConfigProvider;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsActive {

    private static final Logger LOG = LoggerFactory.getLogger(JobsActive.class);

    /**
     * Private class holding the job's current activity and status check interval.
     */
    public static class JobsActiveResult {
        public final boolean active;
        public final int statusCheckIntervalSecs;

        public JobsActiveResult(boolean active, int statusCheckIntervalSecs) {
            this.active = active;
            this.statusCheckIntervalSecs = statusCheckIntervalSecs;
        }
    }

    /**
     * Determine if the job is active.
     *
     * @param   jobId       the id of the job to test
     * @return  active      job activity status object
     * @throws  Exception   exceptions thrown by bad request or database exceptions
     */
    public static JobsActiveResult isJobActive(final String partitionId, String jobId) throws Exception {
        boolean active;
        int statusCheckIntervalMillis;

        try {
            LOG.info("isJobActive: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

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
            AppConfig config = AppConfigProvider.getAppConfigProperties();

            //  Get the number of seconds after which it is appropriate to try to confirm that the task has not been cancelled or aborted.
            statusCheckIntervalMillis = Integer.parseInt(config.getStatusCheckTime());

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Check if the specified job is active or not.
            LOG.info("isJobActive: Checking job status...");
            active = databaseHelper.isJobActive(partitionId, jobId);

        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }

        LOG.info("isJobActive: Done.");
        return new JobsActiveResult(active,statusCheckIntervalMillis);
    }

}
