package com.hpe.caf.services.job.api;

import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.exceptions.NotFoundException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BadRequestException.class, NotFoundException.class})
@PowerMockIgnore("javax.management.*")
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
    public void testToResponseException () {

        ApiExceptionMapper aem = new ApiExceptionMapper();

        Exception bre = new Exception("Test Exception");
        Response r = aem.toResponse(bre);

        //  Expected response status should map onto INTERNAL SERVER ERROR (i.e. 500)
        Assert.assertEquals(r.getStatus(),500);

    }
}
