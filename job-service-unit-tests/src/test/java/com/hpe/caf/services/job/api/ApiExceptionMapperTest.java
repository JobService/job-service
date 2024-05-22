/*
 * Copyright 2016-2024 Open Text.
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
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.exceptions.NotFoundException;
import com.hpe.caf.services.job.exceptions.ServiceUnavailableException;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.ShutdownSignalException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@RunWith(MockitoJUnitRunner.class)
public final class ApiExceptionMapperTest {

    @Test
    public void testToResponseBadRequestException () {

        ApiExceptionMapper aem = new ApiExceptionMapper();

        BadRequestException bre = new BadRequestException("Test BadRequestException");
        Response r = aem.toResponse(bre);

        //  Expected response status should map onto BAD REQUEST (i.e. 400)
        Assert.assertEquals(r.getStatus(),400);

    }

    @Test
    public void testToResponseNotFoundException () {

        ApiExceptionMapper aem = new ApiExceptionMapper();

        NotFoundException bre = new NotFoundException("Test NotFoundException");
        Response r = aem.toResponse(bre);

        //  Expected response status should map onto NOT FOUND (i.e. 404)
        Assert.assertEquals(r.getStatus(),404);

    }

    @Test
    public void testToResponseServiceUnavailableException () {

        ApiExceptionMapper aem = new ApiExceptionMapper();

        ServiceUnavailableException sue = new ServiceUnavailableException("Test ServiceUnavailableException");
        Response r = aem.toResponse(sue);

        //  Expected response status should map onto SERVICE UNAVAILABLE (i.e. 503)
        Assert.assertEquals(r.getStatus(),503);

    }

    @Test
    public void testToResponseEOFException () {

        ApiExceptionMapper aem = new ApiExceptionMapper();

        UnrecognizedPropertyException sue = new UnrecognizedPropertyException(null,"Test ServiceUnavailableException", null, null, "dd"
                , null);
        Response r = aem.toResponse(sue);

        //  Expected response status should map onto SERVICE UNAVAILABLE (i.e. 400)
        Assert.assertEquals(400, r.getStatus());

    }

    @Test
    public void testToResponseIOException () {

        final ApiExceptionMapper aem = new ApiExceptionMapper();

        final Exception bre = new IOException("Test Exception");
        final Response r = aem.toResponse(bre);

        //  Expected response status should map onto INTERNAL SERVER ERROR (i.e. 500)
        Assert.assertEquals(503, r.getStatus());

    }


    @Test
    public void testToResponseTimeoutException () {

        final ApiExceptionMapper aem = new ApiExceptionMapper();

        final Exception bre = new TimeoutException("Test Exception");
        final Response r = aem.toResponse(bre);

        //  Expected response status should map onto INTERNAL SERVER ERROR (i.e. 500)
        Assert.assertEquals(503, r.getStatus());

    }

    @Test
    public void testToResponseAlreadyClosedException () {

        final ApiExceptionMapper aem = new ApiExceptionMapper();

        final Exception bre = new AlreadyClosedException(new ShutdownSignalException(true, true, null, null), null);
        final Response r = aem.toResponse(bre);

        //  Expected response status should map onto INTERNAL SERVER ERROR (i.e. 500)
        Assert.assertEquals(503, r.getStatus());

    }


    @Test
    public void testToResponseException () {

        ApiExceptionMapper aem = new ApiExceptionMapper();

        Exception bre = new Exception("Test Exception");
        Response r = aem.toResponse(bre);

        //  Expected response status should map onto INTERNAL SERVER ERROR (i.e. 500)
        Assert.assertEquals(r.getStatus(),500);

    }
}
