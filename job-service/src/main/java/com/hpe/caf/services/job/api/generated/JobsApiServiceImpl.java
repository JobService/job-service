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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hpe.caf.services.job.api.*;
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.api.generated.model.NewJob;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.Map;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-02-29T10:25:31.219Z")
public class JobsApiServiceImpl extends JobsApiService {

    @Override
    public Response getJobs(final String partitionId, final String jobIdStartsWith, final String statusType,
                            final Integer limit, final Integer offset, String cAFCorrelationId, String sort,
                            final String label, final String filter,
                            SecurityContext securityContext)
            throws Exception {
        final Job[] jobs = JobsGet.getJobs(partitionId, jobIdStartsWith, statusType, limit, offset, sort, label, filter);
        return Response.ok().entity(jobs).build();
    }

    @Override
    public Response getJob(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        Job job = JobsGetById.getJob(partitionId, jobId);
        return Response.ok().entity(job).build();
    }

    @Override
    public Response createOrUpdateJob(final String partitionId, String jobId, NewJob newJob, String cAFCorrelationId, SecurityContext securityContext, UriInfo uriInfo)
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
    public Response deleteJob(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        JobsDelete.deleteJob(partitionId, jobId);
        return Response.noContent().build();
    }

    @Override
    public Response deleteJobs(final String partitionId, final String jobIdStartsWith, final String statusType, final String label,
                               final String filter, final String cAFCorrelationId, final SecurityContext securityContext)
            throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final Long value = JobsDelete.deleteJobs(partitionId, jobIdStartsWith, statusType, label, filter);
        final ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("jobsDeleted", value);

        return Response.ok().entity(jsonNode).build();
    }

    @Override
    public Response cancelJob(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        JobsCancel.cancelJob(partitionId, jobId);
        return Response.noContent().build();
    }

    @Override
    public Response cancelJobs(final String partitionId, final String jobIdStartsWith, final String label, final String filter,
                               final String cAFCorrelationId, final SecurityContext securityContext) throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final Long value = JobsCancel.cancelJobs(partitionId, jobIdStartsWith, label, filter);
        final ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("jobsCanceled", value);

        return Response.ok().entity(jsonNode).build();
    }

    @Override
    public Response pauseJob(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        JobsPause.pauseJob(partitionId, jobId);
        return Response.noContent().build();
    }

    @Override
    public Response resumeJob(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        JobsResume.resumeJob(partitionId, jobId);
        return Response.noContent().build();
    }

    @Override
    public Response getJobActive(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        JobsActive.JobsActiveResult result = JobsActive.isJobActive(partitionId, jobId);

        CacheControl cc = new CacheControl();
        cc.setMaxAge(result.statusCheckIntervalSecs);

        return Response.ok().header("CacheableJobStatus", true).entity(result.active).cacheControl(cc).build();
    }

    @Override
    public Response getJobStatus(final String partitionId, final String jobId, final String cAFCorrelationId,
                                 final SecurityContext securityContext)
            throws Exception {
        final JobsStatus.JobsStatusResult jobStatusResult = JobsStatus.getJobStatus(partitionId, jobId);

        final CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(jobStatusResult.statusCheckIntervalSecs);

        return Response.ok().header("CacheableJobStatus", true).entity(jobStatusResult.jobStatus).cacheControl(cacheControl).build();
    }

    @Override
    public Response ping() throws Exception
    {
        return Response.ok("{\"success\" : true}").build();
    }
}
