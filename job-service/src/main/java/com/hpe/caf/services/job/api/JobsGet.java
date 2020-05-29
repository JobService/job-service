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

import com.hpe.caf.services.configuration.AppConfigProvider;
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.api.generated.model.JobSortField;
import com.hpe.caf.services.job.api.generated.model.SortDirection;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public final class JobsGet {

    private static final Logger LOG = LoggerFactory.getLogger(JobsGet.class);
    private static final JobSortField DEFAULT_SORT_FIELD = JobSortField.CREATE_DATE;
    private static final SortDirection DEFAULT_SORT_DIRECTION = SortDirection.DESCENDING;

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
    public static Job[] getJobs(final String partitionId, final String jobId, final String statusType, Integer limit, final Integer offset, final String sort) throws Exception {
        return JobsGet.getJobs(partitionId, jobId, statusType, limit, offset, sort, null);
    }

    /**
     * Gets a list of jobs from the job database specified by environment variable configuration.
     *
     * @param jobId         optional id of the job to get
     * @param statusType    optional status of the job
     * @param limit         optional limit of jobs to return per page
     * @param offset        optional offset from which to return page of jobs
     * @param labelExists   optional metadata to filter against.
     * @return  jobs        list of jobs
     * @throws Exception    bad request or database exceptions
     */
    public static Job[] getJobs(final String partitionId, final String jobId, final String statusType, Integer limit,
                                final Integer offset, final String sort, final String labelExists) throws Exception {

        Job[] jobs;

        try {
            LOG.info("getJobs: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

            final JobSortField sortField;
            final SortDirection sortDirection;

            if (sort == null) {
                sortField = DEFAULT_SORT_FIELD;
                sortDirection = DEFAULT_SORT_DIRECTION;

            } else {
                final String[] sortParts = sort.split(":", 2);
                if (sortParts.length != 2) {
                    throw new BadRequestException("Invalid format for sort: " + sort);
                }
                sortField = JobSortField.fromApiValue(sortParts[0]);
                if (sortField == null) {
                    throw new BadRequestException("Invalid value for sort field: " + sortParts[0]);
                }
                sortDirection = SortDirection.fromApiValue(sortParts[1]);
                if (sortDirection == null) {
                    throw new BadRequestException(
                        "Invalid value for sort direction: " + sortParts[1]);
                }
            }

            List<String> labelValues = null;
            if(labelExists != null && !labelExists.isEmpty()) {
                final String[] split = labelExists.split(",");
                labelValues = Arrays.asList(split);
            }

            //  Get app config settings.
            LOG.debug("getJobs: Reading database connection properties...");
            AppConfig config = AppConfigProvider.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Get list of job definitions in the system.
            LOG.info("getJobs: Getting list of job definitions...");
            if (limit == null || limit <= 0) {
                limit = config.getDefaultPageSize();
            }
            jobs = databaseHelper.getJobs(
                partitionId, jobId, statusType, limit, offset, sortField, sortDirection, labelValues);
        } catch (Exception e) {
            LOG.error("Error - ", e);
            throw e;
        }


        LOG.info("getJobs: Done.");
        return jobs;
    }
}
