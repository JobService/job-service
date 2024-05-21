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

import com.hpe.caf.services.configuration.AppConfigProvider;
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsGetById {

    private static final Logger LOG = LoggerFactory.getLogger(JobsGetById.class);

    /**
     * Get the job with the specified job id.
     *
     * @param   jobId       job id of the job to return
     * @return  job         the job
     * @throws  Exception   bad request or database exception
     */
    public static Job getJob(final String partitionId, String jobId) throws Exception {

        Job job;

        try {
            LOG.debug("getJobById: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

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
            AppConfig config = AppConfigProvider.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = DatabaseHelperFactory.createDatabaseHelper(config);

            //  Get the job definition for the specified job id.
            LOG.debug("getJobById: Getting job definition...");
            job = databaseHelper.getJob(partitionId, jobId);

            LOG.debug("getJobById: Done.");
            if (null != job) {
                LOG.info("Job progress {}/{} status {}  progression {}", partitionId, jobId, job.getStatus(),
                        job.getPercentageComplete());
            }
            return job;

        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }
    }
}
