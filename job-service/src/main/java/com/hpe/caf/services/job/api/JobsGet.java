/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.configuration.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsGet {

    private static final Logger LOG = LoggerFactory.getLogger(JobsGet.class);

    /**
     * Gets a list of jobs from the job database specified by environment variable configuration.
     *
     * @param jobId         optional id of the job to get
     * @param statusType    optional status of the job
     * @param limit         optional limit of jobs to return per page
     * @param offset        optional offset from which to return page of jobs
     * @return  jobs        list of jobs
     * @throws Exception    bad request or database exceptions
     */
    public static Job[] getJobs(final String jobId, final String statusType, Integer limit, final Integer offset) throws Exception {

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
            if (limit == null || limit <= 0) {
                limit = config.getDefaultPageSize();
            }
            jobs = databaseHelper.getJobs(jobId, statusType, limit, offset);
        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }

        LOG.info("getJobs: Done.");
        return jobs;
    }
}
