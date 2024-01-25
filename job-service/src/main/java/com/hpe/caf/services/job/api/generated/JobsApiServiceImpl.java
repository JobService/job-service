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
package com.hpe.caf.services.job.api.generated;

import com.hpe.caf.services.job.api.*;
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.api.generated.model.NewJob;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.Map;

public class JobsApiServiceImpl implements JobsApi {

    private final UriInfo uriInfo;

    public JobsApiServiceImpl(@Context final UriInfo uriInfo)
    {
        this.uriInfo = uriInfo;
    }

    @Override
    public Response getJobs(final String partitionId, final String jobIdStartsWith, final String statusType,
                            final Integer limit, final Integer offset, final String sort,
                            final String label, final String filter)
            throws Exception {
        final Job[] jobs = JobsGet.getJobs(partitionId, jobIdStartsWith, statusType, limit, offset, sort, label, filter);
        return Response.ok().entity(jobs).build();
    }

    @Override
    public Response getJobsCount(final String partitionId, final String jobIdStartsWith, final String statusType, final String filter)
            throws Exception {
        final Long jobsCount = JobsStatsGetCount.getJobsCount(partitionId, jobIdStartsWith, statusType, filter);
        return Response.ok().entity(jobsCount).build();
    }

    @Override
    public Response getJob(final String partitionId, final String jobId)
            throws Exception {
        Job job = JobsGetById.getJob(partitionId, jobId);
        return Response.ok().entity(job).build();
    }

    @Override
    public Response createOrUpdateJob(final String partitionId, final String jobId, final NewJob newJob)
            throws Exception {
        String createOrUpdate = JobsPut.createOrUpdateJob(partitionId, jobId, newJob);
        if (createOrUpdate.equals("create")) {
            //  Return HTTP 201 for successful create.
            return Response.created(uriInfo.getRequestUri()).build();
        } else {
            //  Must be update - return HTTP 204 for successful update.
            return Response.noContent().build();
        }
    }

    @Override
    public Response deleteJob(final String partitionId, final String jobId)
            throws Exception {
        JobsDelete.deleteJob(partitionId, jobId);
        return Response.noContent().build();
    }

    @Override
    public Response deleteJobs(final String partitionId, final String jobIdStartsWith, final String label, final String filter)
            throws Exception
    {
        final Long value = JobsDelete.deleteJobs(partitionId, jobIdStartsWith, label, filter);
        final Map<String, Long> resultMap = Collections.singletonMap("jobsDeleted", value);

        return Response.ok().entity(resultMap).build();
    }

    @Override
    public Response cancelJob(final String partitionId, final String jobId)
            throws Exception {
        JobsCancel.cancelJob(partitionId, jobId);
        return Response.noContent().build();
    }

    @Override
    public Response cancelJobs(final String partitionId, final String jobIdStartsWith, final String label, final String filter)
            throws Exception
    {
        final Long value = JobsCancel.cancelJobs(partitionId, jobIdStartsWith, label, filter);
        final Map<String, Long> resultMap = Collections.singletonMap("jobsCanceled", value);

        return Response.ok().entity(resultMap).build();
    }

    @Override
    public Response pauseJob(final String partitionId, final String jobId)
            throws Exception {
        JobsPause.pauseJob(partitionId, jobId);
        return Response.noContent().build();
    }

    @Override
    public Response resumeJob(final String partitionId, final String jobId)
            throws Exception {
        JobsResume.resumeJob(partitionId, jobId);
        return Response.noContent().build();
    }

    @Override
    public Response getJobActive(final String partitionId, final String jobId)
            throws Exception {
        JobsActive.JobsActiveResult result = JobsActive.isJobActive(partitionId, jobId);

        CacheControl cc = new CacheControl();
        cc.setMaxAge(result.statusCheckIntervalSecs);

        return Response.ok().header("CacheableJobStatus", true).entity(result.active).cacheControl(cc).build();
    }

    @Override
    public Response getJobStatus(final String partitionId, final String jobId)
            throws Exception {
        final JobsStatus.JobsStatusResult jobStatusResult = JobsStatus.getJobStatus(partitionId, jobId);

        final CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(jobStatusResult.statusCheckIntervalSecs);

        return Response.ok().header("CacheableJobStatus", true).entity(jobStatusResult.jobStatus).cacheControl(cacheControl).build();
    }

    @Override
    public Response ping() throws Exception
    {
        return Response.ok("{\"success\" : true}", MediaType.APPLICATION_JSON).build();
    }
}
