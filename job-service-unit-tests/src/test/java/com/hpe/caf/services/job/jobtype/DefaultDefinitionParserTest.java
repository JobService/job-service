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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultDefinitionParserTest {
    /**
     * Mock application configuration.
     */
    private AppConfig appConfig;

    /**
     * Setup AppConfig mock to have the base configuration required for a job type
     */
    private void setupValidConfig() {
        Mockito.when(appConfig.getJobProperty("TASK_PIPE"))
            .thenReturn("basic task pipe");
        Mockito.when(appConfig.getJobProperty("TARGET_PIPE"))
            .thenReturn("basic target pipe");
        Mockito.when(appConfig.getJobProperty("test1"))
                .thenReturn("classifier");
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

    @BeforeEach
    public void setUp() {
        appConfig = Mockito.mock(AppConfig.class);
        loadTaskScriptSchema();
    }

    private void loadTaskScriptSchema() {
        TaskScriptSchemaInitialization.initialize();
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testBasicDefinition() throws Exception {
        setupValidConfig();
        final JobType jobType =
            new DefaultDefinitionParser(appConfig).parse("basic-id", getDefinition("basic"));
        final WorkerAction task =
            jobType.buildTask("partition id", "job id", NullNode.getInstance());

        assertEquals("basic-id", jobType.getId(), "should use provided ID");
        assertEquals("basic classifier", task.getTaskClassifier(), "should parse classifier");
        assertEquals(74, task.getTaskApiVersion().intValue(), "should parse api version");
        assertEquals("basic task pipe", task.getTaskPipe(), "should get task pipe from config");
        assertEquals("basic target pipe", task.getTargetPipe(), "should get target pipe from config");

        // should fill in params schema that expects null
        Assertions.assertThrows(BadRequestException.class, () -> jobType.buildTask("partition id", "job id", TextNode.valueOf("not null params")));
    }

    @Test
    public void testConstantDefinition() throws Exception
    {
        setupValidConfig();
        final JobType jobType = new DefaultDefinitionParser(appConfig).parse("basic-id", getDefinition("constant"));
        final WorkerAction task = jobType.buildTask("partition id", "job id", NullNode.getInstance());
        final String expectedTaskData = "{cfg={TARGET_PIPE=basic target pipe,TASK_PIPE=basic task pipe}, taskMessageParams={" +
                "graaljs:setResponse.js=function onProcessTask(e){console.log('hello world!');}}}";
        assertEquals(expectedTaskData.replaceAll("\\s+", ""), task.getTaskData().toString().replaceAll("\\s+", ""),
                "Constant values should be used for replacement");
    }

    @Test
    public void testEmptyConfigCompilesToEmptyMap() throws Exception {
        final JobType jobType
            = new DefaultDefinitionParser(appConfig).parse("basic-id", getDefinition("empty-config"));
        final WorkerAction task
            = jobType.buildTask("partition id", "job id", NullNode.getInstance());

        assertEquals("basic-id", jobType.getId(), "should use provided ID");
        assertEquals("basic classifier", task.getTaskClassifier(), "should parse classifier");
        assertEquals(74, task.getTaskApiVersion().intValue(), "should parse api version");
        assertEquals("task pipe not from config", task.getTaskPipe(), "should parse task pipe");
        assertEquals("target pipe not from config", task.getTargetPipe(), "should parse target");

        final Map<String, Map<String, String>> taskData = JobTypeTestUtil.objectMapper.convertValue(
            task.getTaskData(), new TypeReference<Map<String, Map<String, String>>>(){});
        assertEquals(Collections.EMPTY_MAP, taskData.get("cfg"), "should fill in empty configuration");
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testInputStreamError() throws Exception {
        setupValidConfig();
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> new DefaultDefinitionParser(appConfig).parse("id", new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("disk error");
            }
        }));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testInvalidYamlSyntax() throws Exception {
        setupValidConfig();
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> new DefaultDefinitionParser(appConfig).parse("id", getDefinition("invalid-yaml-syntax")));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testInvalidTaskScriptSyntax() throws Exception {
        setupValidConfig();
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> new DefaultDefinitionParser(appConfig).parse("id", getDefinition("invalid-taskscript-syntax")));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testExtraProperty() throws Exception {
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> new DefaultDefinitionParser(appConfig).parse("id", getDefinition("extra-property")));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testMissingTaskClassifier() throws Exception {
        setupValidConfig();
        final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("missing-taskclassifier"));
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> jobType.buildTask("partition id", "id", NullNode.getInstance()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testTaskClassifierWithWrongType() throws Exception {
        setupValidConfig();
        final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("wrongtype-taskclassifier"));
       Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () ->  jobType.buildTask("partition id", "id", NullNode.getInstance()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testEmptyStringTaskClassifier() throws Exception {
        setupValidConfig();
        final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("empty-taskclassifier"));
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> jobType.buildTask("partition id", "id", NullNode.getInstance()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testMissingTaskApiVersion() throws Exception {
        setupValidConfig();
        final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("missing-taskapiversion"));
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> jobType.buildTask("partition id", "id", NullNode.getInstance()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testTaskApiVersionWithWrongType() throws Exception {
        setupValidConfig();
        final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("wrongtype-taskapiversion"));
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> jobType.buildTask("partition id", "id", NullNode.getInstance()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testMissingTaskData() throws Exception {
        setupValidConfig();
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> {
            final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("missing-taskdata"));
            jobType.buildTask("partition id", "id", NullNode.getInstance());
        });
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testTaskDataWithWrongType() throws Exception {
        setupValidConfig();
        final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("wrongtype-taskdata"));
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> jobType.buildTask("partition id", "id", NullNode.getInstance()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testTaskPipeMissingFromConfig() throws Exception {
        Mockito.when(appConfig.getJobProperty("TASK_PIPE")).thenReturn(null);
        Mockito.when(appConfig.getJobProperty("TARGET_PIPE")).thenReturn("basic target pipe");
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> {
            final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("basic"));
            Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> jobType.buildTask("partition id", "id", NullNode.getInstance()));
        });
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testEmptyStringTaskPipe() throws Exception {
        Mockito.when(appConfig.getJobProperty("TASK_PIPE")).thenReturn("");
        Mockito.when(appConfig.getJobProperty("TARGET_PIPE")).thenReturn("basic target pipe");
        final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("basic"));
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> jobType.buildTask("partition id", "id", NullNode.getInstance()));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testTargetPipeMissingFromConfig() throws Exception {
        Mockito.when(appConfig.getJobProperty("TASK_PIPE")).thenReturn("basic task pipe");
        Mockito.when(appConfig.getJobProperty("TARGET_PIPE")).thenReturn(null);
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> {
            final JobType jobType = new DefaultDefinitionParser(appConfig).parse("id", getDefinition("basic"));
            jobType.buildTask("partition id", "id", NullNode.getInstance());
        });
    }

    @Test
    public void testConfigurationProperties() throws Exception {
        setupValidConfig();
        final JobType jobType =
            new DefaultDefinitionParser(appConfig).parse("id", getDefinition("config"));
        final WorkerAction task =
            jobType.buildTask("partition id", "job id", NullNode.getInstance());
        final Map<String, Map<String, String>> taskData = JobTypeTestUtil.objectMapper.convertValue(
            task.getTaskData(), new TypeReference<Map<String, Map<String, String>>>() {});

        final Map<String, String> expectedConfig = new HashMap<>();
        expectedConfig.put("TASK_PIPE", "basic task pipe");
        expectedConfig.put("TARGET_PIPE", "basic target pipe");
        assertEquals(expectedConfig, taskData.get("cfg"), "should retrieve configuration and provide it to the task script");
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testConfigurationPropertiesWithMissingName() throws Exception {
        setupValidConfig();
        Mockito.when(appConfig.getJobProperty("prop_a")).thenReturn("value a");
        Mockito.when(appConfig.getJobProperty("prop_b")).thenReturn("value b");
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> new DefaultDefinitionParser(appConfig).parse("id", getDefinition("missing-config-name")));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testJobParametersSchemaValidation() throws Exception {
        setupValidConfig();
        final JobType jobType =
            new DefaultDefinitionParser(appConfig).parse("id", getDefinition("job-parameters-schema-expects-string"));

        // expects string - should not throw with string
        jobType.buildTask("partition id", "job id", TextNode.valueOf("params"));

        // expects string - should throw with number
        Assertions.assertThrows(BadRequestException.class, () -> jobType.buildTask("partition id", "job id", IntNode.valueOf(123)));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testMissingTaskScript() throws Exception {
        setupValidConfig();
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> new DefaultDefinitionParser(appConfig).parse("id", getDefinition("missing-taskscript")));
    }

}
