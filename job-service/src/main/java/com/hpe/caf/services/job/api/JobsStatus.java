/*
 * Copyright 2016-2024 Open Text.
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
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.api.generated.model.JobStatus;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsStatus {

    private static final Logger LOG = LoggerFactory.getLogger(JobsStatus.class);

    public static class JobsStatusResult {
        public final JobStatus jobStatus;
        public final int statusCheckIntervalSecs;

        public JobsStatusResult(final JobStatus jobStatus, final int statusCheckIntervalSecs) {
            this.jobStatus = jobStatus;
            this.statusCheckIntervalSecs = statusCheckIntervalSecs;
        }
    }

    public static JobsStatusResult getJobStatus(final String partitionId, final String jobId) throws Exception {
        JobStatus jobStatus;
        int statusCheckIntervalSeconds;

        try {
            LOG.debug("getJobStatus: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

            //  Make sure the job id has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(jobId)) {
                LOG.error("getJobStatus: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
            }

            //  Make sure the job id does not contain any invalid characters.
            if (ApiServiceUtil.containsInvalidCharacters(jobId)) {
                LOG.error("getJobStatus: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
            }

            //  Get app config settings.
            LOG.debug("getJobStatus: Reading database connection properties...");
            AppConfig config = AppConfigProvider.getAppConfigProperties();

            //  Get the number of seconds after which it is appropriate to try to confirm that the task has not been cancelled or aborted.
            statusCheckIntervalSeconds = Integer.parseInt(config.getStatusCheckIntervalSeconds());

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Get the status of the specified job.
            LOG.debug("getJobStatus: Getting job status...");
            jobStatus = databaseHelper.getJobStatus(partitionId, jobId);

        } catch (final Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }

        LOG.debug("getJobStatus: Done.");
        return new JobsStatusResult(jobStatus, statusCheckIntervalSeconds);
    }
}
