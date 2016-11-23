package com.hpe.caf.services.job.api.generated;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import com.hpe.caf.services.job.api.generated.model.NewJob;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-02-29T10:25:31.219Z")
public abstract class JobsApiService {

      public abstract Response getJobs(final String jobIdStartsWith, final Integer statusType, final Integer limit, final Integer offset, String cAFCorrelationId,SecurityContext securityContext)
              throws Exception;

      public abstract Response getJob(String jobId,String cAFCorrelationId,SecurityContext securityContext)
              throws Exception;

      public abstract Response createOrUpdateJob(String jobId,NewJob newJob,String cAFCorrelationId,SecurityContext securityContext,UriInfo uriInfo)
              throws Exception;

      public abstract Response deleteJob(String jobId,String cAFCorrelationId,SecurityContext securityContext)
              throws Exception;

      public abstract Response cancelJob(String jobId,String cAFCorrelationId,SecurityContext securityContext)
              throws Exception;

      public abstract Response getJobActive(String jobId,String cAFCorrelationId,SecurityContext securityContext)
              throws Exception;

}
