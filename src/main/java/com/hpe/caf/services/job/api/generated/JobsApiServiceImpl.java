package com.hpe.caf.services.job.api.generated;

import com.hpe.caf.services.job.api.*;
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.api.generated.model.NewJob;

import javax.ws.rs.core.*;
import java.net.URI;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-02-29T10:25:31.219Z")
public class JobsApiServiceImpl extends JobsApiService {

    @Override
    public Response getJobs(String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        Job[] jobs = JobsGet.getJobs();
        return Response.ok().entity(jobs).build();
    }

    @Override
    public Response getJob(String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        Job job = JobsGetById.getJob(jobId);
        return Response.ok().entity(job).build();
    }

    @Override
    public Response createOrUpdateJob(String jobId, NewJob newJob, String cAFCorrelationId, SecurityContext securityContext, UriInfo uriInfo)
            throws Exception {
        String createOrUpdate = JobsPut.createOrUpdateJob(jobId, newJob);
        if (createOrUpdate.equals("create")) {
            //  Return HTTP 201 for successful create.
            return Response.created(uriInfo.getRequestUri()).build();
        }
        else {
            //  Must be update - return HTTP 204 for successful update.
            return Response.noContent().build();
        }
    }

    @Override
    public Response deleteJob(String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        JobsDelete.deleteJob(jobId);
        return Response.noContent().build();
    }

    @Override
    public Response cancelJob(String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        JobsCancel.cancelJob(jobId);
        return Response.noContent().build();
    }

    @Override
    public Response getJobActive(String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        boolean isActive = JobsActive.isJobActive(jobId);
        return Response.ok().entity(isActive).build();
    }

}
