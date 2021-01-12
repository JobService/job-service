/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsStatsGetCount {

    private static final Logger LOG = LoggerFactory.getLogger(JobsStatsGetCount.class);

    /**
     * Returns the number of jobs. The jobId parameter can be used to filter jobs by providing an expression, for example "j1%" will
     * provide jobs beginning with "j1". The statusType parameter can be used to further filter the status of the jobs.
     *
     * @param   jobId       expression for filtering jobs to be counted
     * @param   statusType  further filtering of jobs with the provided status
     * @param   filter      further filtering using the provided RSQL filter expression
     * @return  jobsCount   the number of jobs matching the expression
     * @throws  Exception   bad request or database exceptions
     */
    public static long getJobsCount(final String partitionId, final String jobId, final String statusType, final String filter)
        throws Exception {

        long jobsCount;

        try {
            LOG.debug("getJobsCount: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

            //  Get app config settings.
            LOG.debug("getJobsCount: Reading database connection properties...");
            AppConfig config = AppConfigProvider.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);
            final String convertedFilter = RsqlToSqlUtils.convertToSqlSyntax(filter);

            //  Get number of job definitions in the system.
            LOG.debug("getJobsCount: Getting number of job definitions...");
            jobsCount = databaseHelper.getJobsCount(partitionId, jobId, statusType, convertedFilter);
        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }

        LOG.debug("getJobsCount: Done.");
        return jobsCount;
    }
}
