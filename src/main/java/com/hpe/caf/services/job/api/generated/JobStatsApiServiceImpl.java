package com.hpe.caf.services.job.api.generated;

import com.hpe.caf.services.job.api.*;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.exceptions.NotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-02-29T10:25:31.219Z")
public class JobStatsApiServiceImpl extends JobStatsApiService {

    @Override
    public Response getJobStatsCount(final String jobIdStartsWith, final String statusType, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        try {
            Long jobsCount = JobsStatsGetCount.getJobsCount(jobIdStartsWith, statusType);
            return Response.ok().entity(jobsCount).build();
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(e.getMessage())).build();
        }
    }

}
