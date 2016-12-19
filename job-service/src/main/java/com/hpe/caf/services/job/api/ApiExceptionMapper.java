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
