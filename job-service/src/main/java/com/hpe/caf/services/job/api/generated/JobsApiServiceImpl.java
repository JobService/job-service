/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import com.hpe.caf.services.job.api.*;
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.exceptions.NotFoundException;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-02-29T10:25:31.219Z")
public class JobsApiServiceImpl extends JobsApiService {

    @Override
    public Response getJobs(final String partitionId, final String jobIdStartsWith, final String statusType, final Integer limit, final Integer offset, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        try {
            Job[] jobs = JobsGet.getJobs(partitionId, jobIdStartsWith, statusType, limit, offset);
            return Response.ok().entity(jobs).build();
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(e.getMessage())).build();
        }
    }

    @Override
    public Response getJob(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        try {
            Job job = JobsGetById.getJob(partitionId, jobId);
            return Response.ok().entity(job).build();
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(e.getMessage())).build();
        }
    }

    @Override
    public Response createOrUpdateJob(final String partitionId, String jobId, NewJob newJob, String cAFCorrelationId, SecurityContext securityContext, UriInfo uriInfo)
            throws Exception {
        try {
            String createOrUpdate = JobsPut.createOrUpdateJob(partitionId, jobId, newJob);
            if (createOrUpdate.equals("create")) {
                //  Return HTTP 201 for successful create.
                return Response.created(uriInfo.getRequestUri()).build();
            } else {
                //  Must be update - return HTTP 204 for successful update.
                return Response.noContent().build();
            }
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(e.getMessage())).build();
        }
    }

    @Override
    public Response deleteJob(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        try {
            JobsDelete.deleteJob(partitionId, jobId);
            return Response.noContent().build();
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(e.getMessage())).build();
        }
    }

    @Override
    public Response cancelJob(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        try {
            JobsCancel.cancelJob(partitionId, jobId);
            return Response.noContent().build();
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(e.getMessage())).build();
        }
    }

    @Override
    public Response getJobActive(final String partitionId, String jobId, String cAFCorrelationId, SecurityContext securityContext)
            throws Exception {
        try {
            JobsActive.JobsActiveResult result = JobsActive.isJobActive(partitionId, jobId);

            CacheControl cc = new CacheControl();
            cc.setMaxAge(result.statusCheckIntervalSecs);

            return Response.ok().header("CacheableJobStatus", true).entity(result.active).cacheControl(cc).build();
        } catch (BadRequestException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new ApiResponseMessage(e.getMessage())).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(e.getMessage())).build();
        }
    }

}
