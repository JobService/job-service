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
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.hpe.caf.services.job.exceptions.BadRequestException;

/**
 * Validate parameters using JSON Schema.
 */
final class JsonSchemaParametersValidator implements ParametersValidator {
    private static final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();

    /**
     * Compiled schema to validate with.
     */
    private final JsonSchema schema;

    /**
     * @param jobTypeId Job type's ID, used for error messages
     * @param schema JSON schema to use for validation, as a JSON representation
     * @throws InvalidJobTypeDefinitionException When the schema is invalid
     */
    public JsonSchemaParametersValidator(final String jobTypeId, final JsonNode schema)
        throws InvalidJobTypeDefinitionException
    {
        try {
            this.schema = jsonSchemaFactory.getJsonSchema(schema);
        } catch (final ProcessingException e) {
            throw new InvalidJobTypeDefinitionException(
                jobTypeId + ": invalid jobParametersSchema", e);
        }
    }

    @Override
    public void validate(final JsonNode parameters) throws BadRequestException {
        final ProcessingReport results;
        try {
            results = schema.validate(parameters);
        } catch (final ProcessingException e) {
            throw new BadRequestException("Invalid job parameters", e);
        }

        if (!results.isSuccess()) {
            final StringBuilder errorMessage = new StringBuilder("Invalid job parameters:");
            for (final ProcessingMessage result : results) {
                if (result.getLogLevel() == LogLevel.ERROR ||
                    result.getLogLevel() == LogLevel.FATAL
                ) {
                    errorMessage.append('\n');
                    errorMessage.append(result.getMessage());
                }
            }

            throw new BadRequestException(errorMessage.toString());
        }
    }

}
