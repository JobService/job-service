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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.configuration.AppConfigException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.util.*;

/**
 * Parser for the standard job type definition format.  Expects a YAML file with the following
 * properties:
 *  - id (string, required): unique identifier for the job type within a job service instance
 *  - taskClassifier (string, required): value to use for `taskClassifier` in the constructed job
 *  - taskApiVersion (integer, required): value to use for `taskApiVersion` in the constructed job
 *  - configurationProperties (list of string, optional):
 *        names of properties to look up in job service configuration.  Checks environment variables
 *        of the form `CAF_JOB_SERVICE_JOB_TYPE_[jobTypeId]_[propertyName]`, where `jobTypeId` and
 *        `propertyName` are transformed to upper-case.
 *  - jobParametersSchema (yaml, optional):
 *        JSON schema used to validate input ('parameters') to `taskDataScript` (below).  If not
 *        provided, input must be `null` or an empty object.  This is provided as directly-embedded
 *        YAML, not a JSON or YAML string.
 *  - taskDataScript (string, required):
 *        JSLT script used to construct task data; see {@link JsltTaskDataBuilder} for a
 *        specification
 *
 * The `taskPipe` and `targetPipe` used in the job (and passed to the `taskDataScript`) are
 * retrieved like values in `configurationProperties`.  That is, looked up in environment variables
 * of the form `CAF_JOB_SERVICE_JOB_TYPE_[jobTypeId]_TASK_PIPE` and
 * `CAF_JOB_SERVICE_JOB_TYPE_[jobTypeId]_TARGET_PIPE`, where `jobTypeId` is transformed to
 * upper-case.
 */
public final class DefaultDefinitionParser implements DefinitionParser {
    /**
     * Used to parse the definition.
     */
    private static final Yaml yaml = new Yaml();
    /**
     * Used to convert `jobParametersSchema` to the expected format.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AppConfig appConfig;

    /**
     * @param appConfig
     */
    public DefaultDefinitionParser(final AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public JobType parse(final InputStream definitionStream)
        throws InvalidJobTypeDefinitionException
    {
        // parse yaml onto a Java object; this fails for unexpected properties and properties with
        // incorrect types, but not missing properties
        final JobTypeDefinition definition;
        try {
            definition = yaml.loadAs(definitionStream, JobTypeDefinition.class);
        } catch (final YAMLException e) {
            throw new InvalidJobTypeDefinitionException("Invalid syntax or format", e);
        }

        // `JobTypeDefinition` getters perform validation, so try to only call them once
        // it throws on missing values and fills in defaults, so there are no checks needed here
        final String id = definition.getId();
        final String taskPipe = definition.getTaskPipe(appConfig);
        final String targetPipe = definition.getTargetPipe(appConfig);
        final ParametersValidator parametersValidator =
            new JsonSchemaParametersValidator(id, definition.getParametersSchema());
        final TaskDataBuilder taskDataBuilder = new JsltTaskDataBuilder(
            id, taskPipe, targetPipe, definition.getConfiguration(appConfig),
            parametersValidator, definition.getTaskDataScript());

        return new JobType(
            id, definition.getTaskClassifier(), definition.getTaskApiVersion(),
            taskPipe, targetPipe, taskDataBuilder);
    }


    /**
     * Temporary container for definition contents, parsed directly from yaml.  The yaml library
     * requires that:
     *  - this class must be public and (because it's nested) static
     *  - setters must be public
     *  - getters matching setter names must use the same types (eg. `getTaskApiVersion` can't
     *    return `int`)
     *
     * Validation (and some parsing) is performed in getters instead of setters, since it's mostly
     * missing-value checks, for which setters wouldn't be called anyway.  Getters never return
     * null - you get the provided value, a default value, or an exception.
     */
    public static final class JobTypeDefinition {
        /**
         * Default value for `jobParametersScript`.  Must be marked as `Map` even though
         * `jobParametersScript` is `Object` so we can build it in the static block.
         */
        private static final Map<String, Object> DEFAULT_JOB_PARAMETERS_SCHEMA;

        private String id;
        private String taskClassifier;
        private Integer taskApiVersion;
        private List<String> configurationProperties;
        private Object jobParametersSchema;
        private String taskDataScript;

        static {
            DEFAULT_JOB_PARAMETERS_SCHEMA = new HashMap<>();
            DEFAULT_JOB_PARAMETERS_SCHEMA.put("type", Arrays.asList("null", "object"));
            DEFAULT_JOB_PARAMETERS_SCHEMA.put("additionalProperties", false);
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getId() throws InvalidJobTypeDefinitionException {
            if (id == null) {
                throw new InvalidJobTypeDefinitionException("Missing property: id");
            }
            return id;
        }

        public void setTaskClassifier(final String taskClassifier) {
            this.taskClassifier = taskClassifier;
        }

        public String getTaskClassifier() throws InvalidJobTypeDefinitionException {
            if (taskClassifier == null) {
                // call the `id` getter in case this is called before `getId`, and it's null
                throw new InvalidJobTypeDefinitionException(
                    getId() + ": missing property: taskClassifier");
            }
            return taskClassifier;
        }

        public void setTaskApiVersion(final Integer taskApiVersion) {
            this.taskApiVersion = taskApiVersion;
        }

        public Integer getTaskApiVersion() throws InvalidJobTypeDefinitionException {
            if (taskApiVersion == null) {
                throw new InvalidJobTypeDefinitionException(
                    getId() + ": missing property: taskApiVersion");
            }
            return taskApiVersion;
        }

        public String getTaskPipe(final AppConfig appConfig)
            throws InvalidJobTypeDefinitionException
        {
            final String taskPipe = appConfig.getJobTypeProperty(getId(), "task_pipe");
            if (taskPipe == null) {
                throw new InvalidJobTypeDefinitionException(
                    getId() + ": task pipe is not configured");
            }
            return taskPipe;
        }

        public String getTargetPipe(final AppConfig appConfig)
            throws InvalidJobTypeDefinitionException
        {
            final String targetPipe = appConfig.getJobTypeProperty(getId(), "target_pipe");
            if (targetPipe == null) {
                throw new InvalidJobTypeDefinitionException(
                    getId() + ": target pipe is not configured");
            }
            return targetPipe;
        }

        public void setConfigurationProperties(final List<String> configurationProperties) {
            this.configurationProperties = configurationProperties;
        }

        public Map<String, String> getConfiguration(final AppConfig appConfig)
            throws InvalidJobTypeDefinitionException
        {
            final List<String> properties = configurationProperties == null ?
                Collections.emptyList() : configurationProperties;

            final Map<String, String> configuration = new HashMap<>();
            for (final String propertyName : properties) {
                final String propertyValue = appConfig.getJobTypeProperty(id, propertyName);
                if (propertyValue == null) {
                    throw new InvalidJobTypeDefinitionException(
                        getId() + ": configuration is not available: " + propertyName);
                }
                // preserve property name case in the output, even though the configuration lookup
                // ignores it
                configuration.put(propertyName, propertyValue);
            }

            return configuration;
        }

        public void setJobParametersSchema(final Object jobParametersSchema) {
            this.jobParametersSchema = jobParametersSchema;
        }

        public JsonNode getParametersSchema()
            throws InvalidJobTypeDefinitionException
        {
            final Object schema = jobParametersSchema == null ?
                DEFAULT_JOB_PARAMETERS_SCHEMA :
                jobParametersSchema;
            try {
                return objectMapper.convertValue(schema, JsonNode.class);
            } catch (final IllegalArgumentException e) {
                // should never happen
                throw new InvalidJobTypeDefinitionException(
                    getId() + ": invalid jobParametersSchema", e);
            }
        }

        public void setTaskDataScript(final String taskDataScript) {
            this.taskDataScript = taskDataScript;
        }

        public String getTaskDataScript() throws InvalidJobTypeDefinitionException {
            if (taskDataScript == null) {
                throw new InvalidJobTypeDefinitionException(
                    getId() + ": missing property: taskDataScript");
            }
            return taskDataScript;
        }

    }

}
