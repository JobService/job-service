/*
 * Copyright 2016-2026 Open Text.
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
package com.github.jobservice.core.api;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.github.jobservice.core.api.generated.ApiResponseMessage;
import com.github.jobservice.core.exceptions.BadRequestException;
import com.github.jobservice.core.exceptions.ForbiddenException;
import com.github.jobservice.core.exceptions.NotFoundException;
import com.github.jobservice.core.exceptions.ServiceUnavailableException;
import com.rabbitmq.client.AlreadyClosedException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ApiExceptionMapper class maps exceptions thrown by the audit management api
 * to response http status codes.
 */
@Provider
public final class ApiExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionMapper.class);

    /**
     * Convert an exception to the appropriate response object.
     *
     * @param   exception   the exception to be converted
     * @return  the response
     */
    @Override
    public Response toResponse(Exception exception) {
        final Response.Status httpStatus;
        String exceptionMessage;

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

        if (httpStatus == Response.Status.INTERNAL_SERVER_ERROR || httpStatus == Response.Status.SERVICE_UNAVAILABLE) {
            LOGGER.error("An API exception occurred while processing the request: {}", exception.getMessage(), exception);
            exceptionMessage = "An Internal Server Error occurred while processing the request";
        }
        else {
            LOGGER.warn("An API exception occurred while processing the request: {}", exception.getMessage(), exception);
            exceptionMessage = exception.getMessage();
        }

        //  Include exception message in response.
        return Response.status(httpStatus)
            .type(MediaType.APPLICATION_JSON)
            .entity(new ApiResponseMessage(exceptionMessage))
            .build();
    }
}
