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
package com.hpe.caf.services.job.jobtype;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefaultDefinitionParserTest {
    /**
     * Mock application configuration.
     */
    private AppConfig appConfig;

    /**
     * Setup AppConfig mock to have the base configuration required for a job type
     *
     * @param typeId
     */
    private void setupValidConfig(final String typeId) {
        Mockito.when(appConfig.getJobTypeProperty(typeId, "task_pipe"))
            .thenReturn("basic task pipe");
        Mockito.when(appConfig.getJobTypeProperty(typeId, "target_pipe"))
            .thenReturn("basic target pipe");
    }

    /**
     * Read a test job type definition from the test resources directory.
     *
     * @param testId Definition filename without extension
     * @return
     */
    private InputStream getDefinition(final String testId) {
        return DefaultDefinitionParserTest.class.getResourceAsStream(
            "/job-type-definitions/" + testId + ".yaml");
    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        appConfig = Mockito.mock(AppConfig.class);
    }

    @Test
    public void testBasicDefinition() throws Exception {
        setupValidConfig("basic");
        final JobType jobType =
            new DefaultDefinitionParser(appConfig).parse(getDefinition("basic"));
        final WorkerAction task =
            jobType.buildTask("partition id", "job id", NullNode.getInstance());

        Assert.assertEquals("should parse type ID", "basic", jobType.getId());
        Assert.assertEquals("should parse classifier",
            "basic classifier", task.getTaskClassifier());
        Assert.assertEquals("should parse api version", 74, task.getTaskApiVersion().intValue());
        Assert.assertEquals("should get task pipe from config",
            "basic task pipe", task.getTaskPipe());
        Assert.assertEquals("should get target pipe from config",
            "basic target pipe", task.getTargetPipe());

        final Map<String, Map<String, String>> taskData = JobTypeTestUtil.objectMapper.convertValue(
            task.getTaskData(), new TypeReference<Map<String, Map<String, String>>>() {});
        Assert.assertEquals("should fill in empty configuration",
            Collections.emptyMap(), taskData.get("cfg"));

        // should fill in params schema that expects null
        expectedException.expect(BadRequestException.class);
        jobType.buildTask("partition id", "job id", TextNode.valueOf("not null params"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testInputStreamError() throws Exception {
        setupValidConfig("basic");
        new DefaultDefinitionParser(appConfig).parse(new InputStream() {
            @Override
            public int read() throws IOException { throw new IOException("disk error"); }
        });
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testInvalidYamlSyntax() throws Exception {
        setupValidConfig("invalid-syntax");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("invalid-syntax"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testExtraProperty() throws Exception {
        setupValidConfig("extra-property");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("extra-property"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testPropertyWithWrongType() throws Exception {
        setupValidConfig("wrong-type");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("wrong-type"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testMissingId() throws Exception {
        setupValidConfig("missing-id");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("missing-id"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testMissingTaskClassifier() throws Exception {
        setupValidConfig("missing-taskclassifier");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("missing-taskclassifier"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testMissingTaskApiVersion() throws Exception {
        setupValidConfig("missing-taskapiversion");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("missing-taskapiversion"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testMissingTaskPipe() throws Exception {
        Mockito.when(appConfig.getJobTypeProperty("basic", "target_pipe"))
            .thenReturn("basic target pipe");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("basic"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testMissingTargetPipe() throws Exception {
        Mockito.when(appConfig.getJobTypeProperty("basic", "task_pipe"))
            .thenReturn("basic task pipe");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("basic"));
    }

    @Test
    public void testConfigurationProperties() throws Exception {
        setupValidConfig("config");
        Mockito.when(appConfig.getJobTypeProperty("config", "prop_a")).thenReturn("value a");
        Mockito.when(appConfig.getJobTypeProperty("config", "prop_b")).thenReturn("value b");
        final JobType jobType =
            new DefaultDefinitionParser(appConfig).parse(getDefinition("config"));
        final WorkerAction task =
            jobType.buildTask("partition id", "job id", NullNode.getInstance());
        final Map<String, Map<String, String>> taskData = JobTypeTestUtil.objectMapper.convertValue(
            task.getTaskData(), new TypeReference<Map<String, Map<String, String>>>() {});

        final Map<String, String> expectedConfig = new HashMap<>();
        expectedConfig.put("prop_a", "value a");
        expectedConfig.put("prop_b", "value b");
        Assert.assertEquals("should retrieve configuration and provide it to the task data script",
            expectedConfig, taskData.get("cfg"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testConfigurationPropertiesWithMissingName() throws Exception {
        setupValidConfig("missing-config-name");
        Mockito.when(appConfig.getJobTypeProperty("missing-config-name", "prop_a"))
            .thenReturn("value a");
        Mockito.when(appConfig.getJobTypeProperty("missing-config-name", "prop_b"))
            .thenReturn("value b");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("missing-config-name"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testConfigurationPropertiesWithPropertyNotConfigured() throws Exception {
        setupValidConfig("config");
        Mockito.when(appConfig.getJobTypeProperty("config", "prop_a")).thenReturn("value a");
        // prop_b not configured
        new DefaultDefinitionParser(appConfig).parse(getDefinition("config"));
    }

    @Test
    public void testSchema() throws Exception {
        setupValidConfig("schema");
        final JobType jobType =
            new DefaultDefinitionParser(appConfig).parse(getDefinition("schema"));

        // expects string - should not throw with string
        jobType.buildTask("partition id", "job id", TextNode.valueOf("params"));

        // expects string - should throw with number
        expectedException.expect(BadRequestException.class);
        jobType.buildTask("partition id", "job id", IntNode.valueOf(123));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testMissingTaskDataScript() throws Exception {
        setupValidConfig("missing-taskdatascript");
        new DefaultDefinitionParser(appConfig).parse(getDefinition("missing-taskdatascript"));
    }

}
