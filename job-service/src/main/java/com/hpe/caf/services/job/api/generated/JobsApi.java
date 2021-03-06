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
package com.hpe.caf.services.job.api.generated;

import io.swagger.annotations.ApiParam;

import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.api.generated.model.NewJob;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/partitions")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the jobs API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-03-03T15:07:30.523Z")
public class JobsApi  {

    private final JobsApiService delegate = JobsApiServiceFactory.getJobsApi();
    private final JobStatsApiService statsDelegate = JobStatsApiServiceFactory.getJobStatsApi();

    @GET
    @Path("/{partitionId}/jobs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Gets the list of jobs", notes = "Returns the list of job definitions defined in the system", response = Job.class, responseContainer = "List", tags={ "Jobs",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Returns the list of jobs", response = Job.class, responseContainer = "List") })

    public Response getJobs(
            @ApiParam(value = "Only allow access to jobs in the container with this name",required=true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "Only those results whose job id starts with this value will be returned") @QueryParam("jobIdStartsWith") String jobIdStartsWith,
            @ApiParam(value = "All - no status filter is applied (Default); NotCompleted - only those results with statuses other than Completed will be returned; Completed - only those results with Completed status will be returned; Inactive - only those results with inactive statuses (i.e. Completed, Failed, Cancelled) will be returned; NotFinished - only those results with unfinished statuses (ie. Active, Paused, Waiting) will be returned.") @QueryParam("statusType") String statusType,
            @ApiParam(value = "The maximum results to return (i.e. page size)") @QueryParam("limit") Integer limit,
            @ApiParam(value = "The starting position from which to return results (useful for paging)") @QueryParam("offset") Integer offset,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services" )@HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,
            @ApiParam(value = "How to order the returned results, in the format <field>:<direction>.  Allowed values for field: jobId, createTime.  Allowed values for direction: asc, desc.") @QueryParam("sort") String sort,
            @ApiParam(value = "Filter jobs with any of the specified labels, in the format label=<labelName>,<labelName>") @QueryParam("labelExist") String label,
            @ApiParam(value = "Filter jobs with the specified RSQL filter criteria") @QueryParam("filter") String filter,
            @Context SecurityContext securityContext)
            throws Exception {
        return delegate.getJobs(partitionId, jobIdStartsWith, statusType, limit, offset, cAFCorrelationId, sort, label, filter, securityContext);
    }

    @GET
    @Path("/{partitionId}/jobs/{jobId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Gets the specified job", notes = "Retrieves information about the specified job", response = Job.class, tags={ "Jobs",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Returns the job data", response = Job.class),

            @io.swagger.annotations.ApiResponse(code = 400, message = "The `jobId` parameter contains invalid characters.", response = Job.class),

            @io.swagger.annotations.ApiResponse(code = 404, message = "The specified job is not found.", response = Job.class) })

    public Response getJob(
            @ApiParam(value = "Only allow access to jobs in the container with this name",required=true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "The identifier of the job",required=true) @PathParam("jobId") String jobId,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services" )@HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,@Context SecurityContext securityContext)
            throws Exception {
        return delegate.getJob(partitionId, jobId,cAFCorrelationId,securityContext);
    }

    @PUT
    @Path("/{partitionId}/jobs/{jobId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Adds a new job", notes = "Creates the specified job using the job definition included in the http body", response = void.class, tags={ "Jobs",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 201, message = "Indicates that the job was successfully created", response = void.class),

            @io.swagger.annotations.ApiResponse(code = 204, message = "Indicates that the job was successfully updated", response = void.class),

            @io.swagger.annotations.ApiResponse(code = 400, message = "The `jobId` parameter contains invalid characters.", response = void.class) })

    public Response createOrUpdateJob(
            @ApiParam(value = "Only allow access to jobs in the container with this name",required=true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "The identifier of the job",required=true) @PathParam("jobId") String jobId,
            @ApiParam(value = "The definition of the job to create" ,required=true) NewJob newJob,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services" )@HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,@Context SecurityContext securityContext, @Context UriInfo uriInfo)
            throws Exception {
        return delegate.createOrUpdateJob(partitionId, jobId,newJob,cAFCorrelationId,securityContext,uriInfo);
    }

    @DELETE
    @Path("/{partitionId}/jobs/{jobId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Deletes the specified job", notes = "Deletes the specified job from the system", response = void.class, tags={ "Jobs" })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 204, message = "Indicates that the job was successfully deleted", response = void.class),

            @io.swagger.annotations.ApiResponse(code = 400, message = "The `jobId` parameter contains invalid characters.", response = void.class),

            @io.swagger.annotations.ApiResponse(code = 404, message = "The specified job is not found.", response = void.class) })

    public Response deleteJob(
            @ApiParam(value = "Only allow access to jobs in the container with this name",required=true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "The identifier of the job",required=true) @PathParam("jobId") String jobId,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services" )@HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,@Context SecurityContext securityContext)
            throws Exception {
        return delegate.deleteJob(partitionId, jobId,cAFCorrelationId,securityContext);
    }

    @POST
    @Path("/{partitionId}/jobs/{jobId}/cancel")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Cancels the job", notes = "Cancels the specified job", response = void.class, tags={ "Jobs",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 204, message = "The cancellation has been accepted", response = void.class),

            @io.swagger.annotations.ApiResponse(code = 400, message = "The `jobId` parameter contains invalid characters.", response = void.class),

            @io.swagger.annotations.ApiResponse(code = 404, message = "The specified job is not found.", response = void.class) })

    public Response cancelJob(
            @ApiParam(value = "Only allow access to jobs in the container with this name",required=true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "The identifier of the job",required=true) @PathParam("jobId") String jobId,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services" )@HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,@Context SecurityContext securityContext)
            throws Exception {
        return delegate.cancelJob(partitionId, jobId,cAFCorrelationId,securityContext);
    }

    @POST
    @Path("/{partitionId}/jobs/{jobId}/pause")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @io.swagger.annotations.ApiOperation(value = "Pauses the job.", notes = "Pauses the specified job.", response = void.class, tags = {"Jobs",})
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 204, message = "The pause request has been accepted.", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 400, message = "The request could not be processed because one or more arguments are invalid.", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "The specified job is not found.", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 503, message = "The request failed due to a database connection error.", response = void.class)})
    public Response pauseJob(
            @ApiParam(value = "Only allow access to jobs in the container with this name", required = true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "The identifier of the job", required = true) @PathParam("jobId") String jobId,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services") @HeaderParam("CAF-Correlation-Id") String cAFCorrelationId, @Context SecurityContext securityContext)
            throws Exception {
        return delegate.pauseJob(partitionId, jobId, cAFCorrelationId, securityContext);
    }

    @POST
    @Path("/{partitionId}/jobs/{jobId}/resume")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @io.swagger.annotations.ApiOperation(value = "Pauses the job.", notes = "Pauses the specified job.", response = void.class, tags = {"Jobs",})
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 204, message = "The resume request has been accepted.", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 400, message = "The request could not be processed because one or more arguments are invalid.", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "The specified job is not found.", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 503, message = "The request failed due to a database connection error.", response = void.class)})
    public Response resumeJob(
            @ApiParam(value = "Only allow access to jobs in the container with this name", required = true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "The identifier of the job", required = true) @PathParam("jobId") String jobId,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services") @HeaderParam("CAF-Correlation-Id") String cAFCorrelationId, @Context SecurityContext securityContext)
            throws Exception {
        return delegate.resumeJob(partitionId, jobId, cAFCorrelationId, securityContext);
    }

    @GET
    @Path("/{partitionId}/jobs/{jobId}/isActive")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Checks if the job is active", notes = "Checks if the specified job is active", response = Boolean.class, tags={ "Jobs" })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Returns whether the job is active", response = Boolean.class),

            @io.swagger.annotations.ApiResponse(code = 400, message = "The `jobId` parameter contains invalid characters.", response = Boolean.class) })

    public Response getJobActive(
            @ApiParam(value = "Only allow access to jobs in the container with this name",required=true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "The identifier of the job",required=true) @PathParam("jobId") String jobId,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services" )@HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,@Context SecurityContext securityContext)
            throws Exception {
        return delegate.getJobActive(partitionId, jobId,cAFCorrelationId,securityContext);
    }

    @GET
    @Path("/{partitionId}/jobs/{jobId}/status")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @io.swagger.annotations.ApiOperation(value = "Gets the status of the job.", notes = "Gets the status of the specified job.", response = Boolean.class, tags = {"Jobs"})
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Returns the status of the job.", response = Boolean.class),
            @io.swagger.annotations.ApiResponse(code = 400, message = "The `jobId` parameter contains invalid characters.", response = Boolean.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "The specified job is not found.", response = void.class),
            @io.swagger.annotations.ApiResponse(code = 503, message = "The request failed due to a database connection error.", response = void.class)})
    public Response getJobStatus(
            @ApiParam(value = "Only allow access to jobs in the container with this name", required = true) @PathParam("partitionId") String partitionId,
            @ApiParam(value = "The identifier of the job", required = true) @PathParam("jobId") String jobId,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services") @HeaderParam("CAF-Correlation-Id") String cAFCorrelationId, @Context SecurityContext securityContext)
            throws Exception {
        return delegate.getJobStatus(partitionId, jobId, cAFCorrelationId, securityContext);
    }

    @GET
    @Path("/{partitionId}/jobStats/count")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Gets a count of jobs", notes = "Returns a count of job definitions defined in the system", response = Long.class, tags={ "Jobs",  })
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 200, message = "Returns the count of jobs", response = Long.class) })

    public Response getJobStatsCount(
        @ApiParam(value = "Only allow access to jobs in the container with this name",required=true) @PathParam("partitionId") String partitionId,
        @ApiParam(value = "Only those results whose job id starts with this value will be returned") @QueryParam("jobIdStartsWith") String jobIdStartsWith,
        @ApiParam(value = "All - no status filter is applied (Default); NotCompleted - only those results with statuses other than Completed will be returned; Completed - only those results with Completed status will be returned; Inactive - only those results with inactive statuses (i.e. Completed, Failed, Cancelled) will be returned; NotFinished - only those results with unfinished statuses (ie. Active, Paused, Waiting) will be returned.") @QueryParam("statusType") String statusType,
        @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services" )@HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,
        @ApiParam(value = "Filter jobs with the specified RSQL filter criteria") @QueryParam("filter") String filter,
        @Context SecurityContext securityContext)
        throws Exception {
        return statsDelegate.getJobStatsCount(partitionId, jobIdStartsWith, statusType, filter, cAFCorrelationId, securityContext);
    }
}
