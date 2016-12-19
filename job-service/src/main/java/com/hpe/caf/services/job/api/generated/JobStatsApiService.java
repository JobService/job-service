package com.hpe.caf.services.job.api.generated;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-02-29T10:25:31.219Z")
public abstract class JobStatsApiService {

    public abstract Response getJobStatsCount(final String jobIdStartsWith, final String statusType, String cAFCorrelationId,SecurityContext securityContext)
            throws Exception;

}
