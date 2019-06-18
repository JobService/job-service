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
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.util.*;

/**
 * Parser for the standard job type definition format.  See the `Job-Types.md` document for a
 * specification.
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
        final Definition definition;
        try {
            definition = yaml.loadAs(definitionStream, Definition.class);
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


    /*
     * The static classes below are temporary containers, parsed directly from yaml.  The yaml
     * library requires that:
     *  - these classes must be public and (because they're nested) static
     *  - setters must be public
     *  - getters matching setter names must use the same types (eg. `getTaskApiVersion` can't
     *    return `int`)
     *
     * Validation (and some parsing) is performed in getters instead of setters, since it's mostly
     * missing-value checks, for which setters wouldn't be called anyway.  Getters never return
     * null - you get the provided value, a default value, or an exception.
     */


    /**
     * Definition, parsed from yaml.
     */
    public static final class Definition {
        /**
         * Default value for `jobParametersScript`.
         */
        private static final Object DEFAULT_JOB_PARAMETERS_SCHEMA =
            Collections.singletonMap("type", "null");

        private String id;
        private String taskClassifier;
        private Integer taskApiVersion;
        private List<ConfigurationProperty> configurationProperties;
        private Object jobParametersSchema;
        private String taskDataScript;

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

        public void setConfigurationProperties(
            final List<ConfigurationProperty> configurationProperties
        ) {
            this.configurationProperties = configurationProperties;
        }

        public Map<String, String> getConfiguration(final AppConfig appConfig)
            throws InvalidJobTypeDefinitionException
        {
            final List<ConfigurationProperty> properties = configurationProperties == null ?
                Collections.emptyList() : configurationProperties;

            final Map<String, String> configuration = new HashMap<>();
            for (final ConfigurationProperty property : properties) {
                final String propertyName = property.getName(this);
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

        public JsonNode getParametersSchema() {
            // should never fail
            return objectMapper.convertValue(
                jobParametersSchema == null ? DEFAULT_JOB_PARAMETERS_SCHEMA : jobParametersSchema,
                JsonNode.class);
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


    /**
     * An item in a definition's `configurationProperties`, parsed from yaml.
     */
    public static final class ConfigurationProperty {
        private String name;
        private String description;

        public void setName(final String name) {
            this.name = name;
        }

        public String getName(final Definition definition)
            throws InvalidJobTypeDefinitionException
        {
            if (name == null) {
                throw new InvalidJobTypeDefinitionException(
                    definition.getId() + ": configurationProperties item: missing property: name");
            }
            return name;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public String getDescription() {
            return description == null ? "" : description;
        }

    }

}
