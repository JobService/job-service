package com.hpe.caf.services.job.api.generated;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-02-29T10:25:31.219Z")
public class JobStatsApiServiceFactory {

    private final static JobStatsApiService service = new JobStatsApiServiceImpl();

    public static JobStatsApiService getJobStatsApi()
    {
        return service;
    }
}
