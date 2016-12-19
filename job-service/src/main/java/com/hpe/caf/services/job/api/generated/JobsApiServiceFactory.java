package com.hpe.caf.services.job.api.generated;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-02-29T10:25:31.219Z")
public class JobsApiServiceFactory {

   private final static JobsApiService service = new JobsApiServiceImpl();

   public static JobsApiService getJobsApi()
   {
      return service;
   }
}
