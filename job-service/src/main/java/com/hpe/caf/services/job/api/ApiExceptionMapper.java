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
package com.hpe.caf.services.job.api;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.hpe.caf.services.job.api.generated.ApiResponseMessage;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.exceptions.ForbiddenException;
import com.hpe.caf.services.job.exceptions.NotFoundException;
import com.hpe.caf.services.job.exceptions.ServiceUnavailableException;
import com.rabbitmq.client.AlreadyClosedException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

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
        final Response.Status httpStatus;
        if (exception instanceof BadRequestException ||
            exception instanceof UnrecognizedPropertyException
        ) {
            httpStatus = Response.Status.BAD_REQUEST;
        } else if (exception instanceof NotFoundException) {
            httpStatus = Response.Status.NOT_FOUND;
        } else if (exception instanceof ForbiddenException) {
            httpStatus = Response.Status.FORBIDDEN;
        } else if (
            exception instanceof ServiceUnavailableException ||
            exception instanceof AlreadyClosedException ||
            exception instanceof TimeoutException ||
            exception instanceof IOException
        ) {
            httpStatus = Response.Status.SERVICE_UNAVAILABLE;
        } else {
            httpStatus = Response.Status.INTERNAL_SERVER_ERROR;
        }

        //  Include exception message in response.
        return Response.status(httpStatus)
            .entity(new ApiResponseMessage(exception.getMessage()))
            .build();
    }
}
