/*
 * Copyright 2016-2025 Open Text.
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
package com.github.jobservice.core.jobtype;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;

public final class JsonSchemaTaskScriptValidator {

    private static JsonSchemaTaskScriptValidator INSTANCE;

    private final Schema compiledTaskScriptSchema;

    private JsonSchemaTaskScriptValidator(final JsonNode taskScriptSchema)
    {
        final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4);
        this.compiledTaskScriptSchema = schemaRegistry.getSchema(taskScriptSchema);
    }

    public static void initialise(final JsonNode taskScriptSchema)
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
        final List<Error> errors = compiledTaskScriptSchema.validate(taskScript);

        if (!errors.isEmpty()) {
            final StringBuilder errorMessage = new StringBuilder("Invalid taskScript:");
            for (final Error error : errors) {
                errorMessage.append('\n').append(error.getMessage());
            }
            throw new InvalidJobTypeDefinitionException(errorMessage.toString());
        }
    }
}
