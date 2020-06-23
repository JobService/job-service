/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
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
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JsltTaskDataBuilderTest {
    private static final ParametersValidator paramValidatorSuccess = params -> {};

    @Test
    public void testBuild() throws Exception {
        final Map<String, String> config = Collections.singletonMap("cfg key", "cfg val");
        final TaskDataBuilder builder = new JsltTaskDataBuilder(
            "type", "task pipe", "target pipe", config, paramValidatorSuccess, ".");
        final JsonNode actualTaskData =
            builder.build(null, "job id", TextNode.valueOf("params"));

        final Map<String, Object> expectedTaskData = new HashMap<>();
        expectedTaskData.put("configuration", config);
        expectedTaskData.put("taskPipe", "task pipe");
        expectedTaskData.put("targetPipe", "target pipe");
        expectedTaskData.put("partitionId", "partition id");
        expectedTaskData.put("jobId", "job id");
        expectedTaskData.put("parameters", "params");
        Assert.assertEquals(JobTypeTestUtil.convertJson(expectedTaskData), actualTaskData);
    }

    @Test
    public void testBuildWithNullTargetPipe() throws Exception {
        final Map<String, String> config = Collections.singletonMap("cfg key", "cfg val");
        final TaskDataBuilder builder = new JsltTaskDataBuilder(
            "type", "task pipe", null, config, paramValidatorSuccess, ".targetPipe");
        final JsonNode actualTaskData =
            builder.build("partition id", "job id", TextNode.valueOf("params"));

        Assert.assertEquals("targetPipe should not be passed to script",
            NullNode.getInstance(), actualTaskData);
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testBuildWithIncorrectScriptSyntax() throws Exception {
        new JsltTaskDataBuilder(
            "type", "task pipe", "target pipe", Collections.emptyMap(), paramValidatorSuccess,
            "not a valid script");
    }

    @Test(expected = BadRequestException.class)
    public void testBuildWithParamValidationError() throws Exception {
        final TaskDataBuilder builder = new JsltTaskDataBuilder(
            "type", "task pipe", "target pipe", Collections.emptyMap(),
            params -> { throw new BadRequestException("invalid params"); },
            ".");
        builder.build("partition id", "job id", TextNode.valueOf("params"));
    }

    // a script which fails by using `error()`
    @Test(expected = BadRequestException.class)
    public void testBuildWithFailingScript() throws Exception {
        final TaskDataBuilder builder = new JsltTaskDataBuilder(
            "type", "task pipe", "target pipe", Collections.emptyMap(), paramValidatorSuccess,
            "error(\"input not quite right\")");
        builder.build("partition id", "job id", TextNode.valueOf("params"));
    }

    // a script which is syntactically correct but fails without explicitly using `error()`
    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testBuildWithInvalidScript() throws Exception {
        final TaskDataBuilder builder = new JsltTaskDataBuilder(
            "type", "task pipe", "target pipe", Collections.emptyMap(), paramValidatorSuccess,
            "{ \"result\": .jobId[\"key\"] }"); // can't index string with string
        builder.build("partition id", "job id", TextNode.valueOf("params"));
    }

    @Test
    public void testBuildWithEmptyObjectInScriptResult() throws Exception {
        final Map<String, String> config = Collections.singletonMap("cfg key", "cfg val");
        final TaskDataBuilder builder = new JsltTaskDataBuilder(
            "type", "task pipe", "target pipe", config, paramValidatorSuccess,
            "{ \"empty\": {} }");
        final JsonNode actualTaskData =
            builder.build("partition id", "job id", TextNode.valueOf("params"));

        Assert.assertEquals(JobTypeTestUtil.convertJson(Collections.emptyMap()), actualTaskData);
    }

}
