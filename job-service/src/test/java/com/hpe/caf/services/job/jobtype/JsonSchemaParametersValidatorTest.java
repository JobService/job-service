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
package com.hpe.caf.services.job.jobtype;

import com.fasterxml.jackson.databind.JsonNode;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.junit.Test;

public class JsonSchemaParametersValidatorTest {

    @Test
    public void testValidJson() throws Exception {
        final JsonNode schema = JobTypeTestUtil.buildJson("{ \"type\": \"number\" }");
        final JsonNode params = JobTypeTestUtil.buildJson("123");
        new JsonSchemaParametersValidator("id", schema).validate(params);
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidJson() throws Exception {
        final JsonNode schema = JobTypeTestUtil.buildJson("{ \"type\": \"number\" }");
        final JsonNode params = JobTypeTestUtil.buildJson("\"not a number\"");
        new JsonSchemaParametersValidator("id", schema).validate(params);
    }

}
