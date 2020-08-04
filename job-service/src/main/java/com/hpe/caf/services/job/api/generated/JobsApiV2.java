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
package com.hpe.caf.services.job.api.generated;

import com.hpe.caf.services.job.api.generated.model.CorrectedFormatJob;
import io.swagger.annotations.ApiParam;

import com.hpe.caf.services.job.api.generated.model.Job;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;

@Path("/v2/partitions")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(description = "the jobs API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-03-03T15:07:30.523Z")
public class JobsApiV2
{

    private final JobsApiService delegate = JobsApiServiceFactory.getJobsApi();

    @GET
    @Path("/{partitionId}/jobs")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @io.swagger.annotations.ApiOperation(
        value = "Gets the list of jobs", notes = "Returns the list of job definitions defined in the system",
        response = CorrectedFormatJob.class, responseContainer = "List", tags = {"Jobs",})
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "Returns the list of jobs", response = CorrectedFormatJob.class,
                                            responseContainer = "List")})

    public Response getJobs(
        @ApiParam(value = "Only allow access to jobs in the container with this name", required = true)
        @PathParam("partitionId") String partitionId,
        @ApiParam(value = "Only those results whose job id starts with this value will be returned")
        @QueryParam("jobIdStartsWith") String jobIdStartsWith,
        @ApiParam(
            value = "All - no status filter is applied (Default); NotCompleted - only those results with statuses other than Completed "
            + "will be returned; Completed - only those results with Completed status will be returned; Inactive - only those results "
            + "with inactive statuses (i.e. Completed, Failed, Cancelled) will be returned; NotFinished - only those results "
            + "with unfinished statuses (ie. Active, Paused, Waiting) will be returned.")
        @QueryParam("statusType") String statusType,
        @ApiParam(value = "The maximum results to return (i.e. page size)") @QueryParam("limit") Integer limit,
        @ApiParam(value = "The starting position from which to return results (useful for paging)") @QueryParam("offset") Integer offset,
        @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services")
        @HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,
        @ApiParam(value = "How to order the returned results, in the format <field>:<direction>.  "
            + "Allowed values for field: jobId, createTime.  Allowed values for direction: asc, desc.")
        @QueryParam("sort") String sort,
        @ApiParam(value = "Filter jobs with any of the specified labels, in the format label=<labelName>,<labelName>")
        @QueryParam("labelExist") String label,
        @ApiParam(value = "Filter jobs with the specified RSQL filter criteria") @QueryParam("filter") String filter,
        @Context SecurityContext securityContext)
        throws Exception
    {
        return delegate.getJobs(partitionId, jobIdStartsWith, statusType, limit, offset, cAFCorrelationId, sort, label, filter,
                                true, securityContext);
    }

    @GET
    @Path("/{partitionId}/jobs/{jobId}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @io.swagger.annotations.ApiOperation(value = "Gets the specified job", notes = "Retrieves information about the specified job",
                                         response = CorrectedFormatJob.class, tags = {"Jobs",})
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "Returns the job data", response = CorrectedFormatJob.class),

        @io.swagger.annotations.ApiResponse(code = 400, message = "The `jobId` parameter contains invalid characters.",
                                            response = CorrectedFormatJob.class),

        @io.swagger.annotations.ApiResponse(code = 404, message = "The specified job is not found.", response = CorrectedFormatJob.class)})

    public Response getJob(
        @ApiParam(value = "Only allow access to jobs in the container with this name", required = true)
        @PathParam("partitionId") String partitionId,
        @ApiParam(value = "The identifier of the job", required = true) @PathParam("jobId") String jobId,
        @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services")
        @HeaderParam("CAF-Correlation-Id") String cAFCorrelationId, @Context SecurityContext securityContext)
        throws Exception
    {
        return delegate.getJob(partitionId, jobId, cAFCorrelationId, true, securityContext);
    }
}
