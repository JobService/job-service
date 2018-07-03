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
