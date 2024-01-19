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
import com.hpe.caf.services.job.api.filter.RsqlToSqlUtils;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public final class JobsDelete {

    private static final Logger LOG = LoggerFactory.getLogger(JobsDelete.class);

    /**
     * Delete the job with the specified job id.
     *
     * @param   jobId       id of the job to delete
     * @throws  Exception   bad request or database exceptions thrown
     */
    public static void deleteJob(final String partitionId, String jobId) throws Exception {
        try {
            LOG.debug("deleteJob: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

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
            AppConfig config = AppConfigProvider.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Delete the specified job from the system.
            LOG.debug("deleteJob: Deleting job...");
            databaseHelper.deleteJob(partitionId, jobId);

            LOG.debug("deleteJob: Done.");

        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }
    }

    public static long deleteJobs(final String partitionId, final String jobIdStartsWith, final String statusType,
                                 final String labelExists, final String filter) throws Exception
    {
        try {
            LOG.debug("deleteJobs: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

            List<String> labelValues = null;
            if (labelExists != null && !labelExists.isEmpty()) {
                final String[] split = labelExists.split(",");
                labelValues = Arrays.asList(split);
            }

            final String filterQuery = RsqlToSqlUtils.convertToSqlSyntax(filter);

            // Get the app config settings.
            LOG.debug("deleteJobs: Reading database connection properties...");
            final AppConfig config = AppConfigProvider.getAppConfigProperties();

            // Get database helper instance
            final DatabaseHelper databaseHelper = new DatabaseHelper(config);

            // Delete the specified jobs.
            LOG.debug("deleteJobs: Deleting the jobs...");
            final int successfulDeletions = databaseHelper.deleteJobs(partitionId, jobIdStartsWith, statusType, labelValues, filterQuery);

            LOG.debug("deleteJobs: Done");
            return successfulDeletions;
        } catch (final Exception e) {
            LOG.error("Error - ", e);
            throw e;
        }
    }
}
