/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.exceptions.NotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * The ApiExceptionMapper class maps exceptions thrown by the audit management api
 * to response http status codes.
 */
@Provider
public final class ApiExceptionMapper implements ExceptionMapper<Exception> {

    /**
     * Convert an exception to the appropriate response object.
     *
     * @param   exception   the exception to be converted
     * @return  the response
     */
    @Override
    public Response toResponse(Exception exception) {
        //  Default response to HTTP 500 (i.e. INTERNAL SERVER ERROR)
        Response.Status httpStatus = Response.Status.INTERNAL_SERVER_ERROR;

        //  Map BadRequestExceptions to HTTP 400 (i.e. BAD REQUEST)
        if (exception instanceof BadRequestException)
            httpStatus = Response.Status.BAD_REQUEST;

        //  Map NotFoundExceptions to HTTP 404 (i.e. NOT FOUND)
        if (exception instanceof NotFoundException)
            httpStatus = Response.Status.NOT_FOUND;

        //  Include exception message in response.
        return Response.status(httpStatus).entity(exception.getMessage())
                .build();
    }
}
