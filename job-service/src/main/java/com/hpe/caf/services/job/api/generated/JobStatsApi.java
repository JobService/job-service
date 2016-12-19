package com.hpe.caf.services.job.api.generated;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;

@Path("/jobStats")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the job stats API")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-03-03T15:07:30.523Z")
public class JobStatsApi  {

    private final JobStatsApiService delegate = JobStatsApiServiceFactory.getJobStatsApi();

    @GET
    @Path("/count")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Gets a count of jobs", notes = "Returns a count of job definitions defined in the system", response = Long.class, tags={ "Jobs",  })
    @io.swagger.annotations.ApiResponses(value = {
            @io.swagger.annotations.ApiResponse(code = 200, message = "Returns the count of jobs", response = Long.class) })

    public Response getJobStatsCount(
            @ApiParam(value = "Only those results whose job id starts with this value will be returned") @QueryParam("jobIdStartsWith") String jobIdStartsWith,
            @ApiParam(value = "All - no status filter is applied (Default); NotCompleted - only those results with statuses other than Completed will be returned; Completed - only those results with Completed status will be returned; Inactive - only those results with inactive statuses (i.e. Completed, Failed, Cancelled) will be returned.") @QueryParam("statusType") String statusType,
            @ApiParam(value = "An identifier that can be used to correlate events that occurred\nacross different CAF services" )@HeaderParam("CAF-Correlation-Id") String cAFCorrelationId,@Context SecurityContext securityContext)
            throws Exception {
        return delegate.getJobStatsCount(jobIdStartsWith, statusType, cAFCorrelationId,securityContext);
    }

}
