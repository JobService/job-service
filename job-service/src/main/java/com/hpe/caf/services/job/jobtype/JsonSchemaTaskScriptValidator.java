/*
 * Copyright 2016-2023 Open Text.
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

public final class JsonSchemaTaskScriptValidator {

    private static JsonSchemaTaskScriptValidator INSTANCE;

    private static final JsonSchemaFactory JSON_SCHEMA_FACTORY = JsonSchemaFactory.byDefault();

    private final JsonSchema compiledTaskScriptSchema;

    private JsonSchemaTaskScriptValidator(final JsonNode taskScriptSchema)
        throws InvalidTaskScriptSchemaException {
        try {
            this.compiledTaskScriptSchema = JSON_SCHEMA_FACTORY.getJsonSchema(taskScriptSchema);
        } catch (final ProcessingException e) {
            throw new InvalidTaskScriptSchemaException("Schema used to validate taskScript is invalid", e);
        }
    }

    public static void initialise(final JsonNode taskScriptSchema) throws InvalidTaskScriptSchemaException
    {
        INSTANCE = new JsonSchemaTaskScriptValidator(taskScriptSchema);
    }

    public static JsonSchemaTaskScriptValidator getInstance()
    {
        if (INSTANCE == null) {
            throw new IllegalStateException("taskScript schema has not been loaded");
        }
        return INSTANCE;
    }

    public void validate(final JsonNode taskScript) throws InvalidJobTypeDefinitionException {
        final ProcessingReport results;
        try {
            results = compiledTaskScriptSchema.validate(taskScript);
        } catch (final ProcessingException e) {
            throw new InvalidJobTypeDefinitionException("Invalid taskScript", e);
        }

        if (!results.isSuccess()) {
            final StringBuilder errorMessage = new StringBuilder("Invalid taskScript:");
            for (final ProcessingMessage result : results) {
                if (result.getLogLevel() == LogLevel.ERROR ||
                    result.getLogLevel() == LogLevel.FATAL
                ) {
                    errorMessage.append('\n');
                    errorMessage.append(result.getMessage());
                }
            }
            throw new InvalidJobTypeDefinitionException(errorMessage.toString());
        }
    }
}
