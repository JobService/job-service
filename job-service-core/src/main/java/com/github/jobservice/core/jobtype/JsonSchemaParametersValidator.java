/*
 * Copyright 2016-2026 Open Text.
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
import com.github.cafapi.common.util.jackson.JacksonMigrationFunctions;
import com.github.jobservice.core.exceptions.BadRequestException;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;

/**
 * Validate parameters using JSON Schema.
 */
final class JsonSchemaParametersValidator implements ParametersValidator {

    /**
     * Compiled schema to validate with.
     */
    private final Schema schema;

    /**
     * @param jobTypeId Job type's ID, used for error messages
     * @param schema JSON schema to use for validation, as a JSON representation
     * @throws InvalidJobTypeDefinitionException When the schema is invalid
     */
    public JsonSchemaParametersValidator(final String jobTypeId, final JsonNode schema)
    {
        final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4);
        this.schema = schemaRegistry.getSchema(JacksonMigrationFunctions.toJackson3(schema));
    }

    @Override
    public void validate(final JsonNode parameters) throws BadRequestException {
        final List<Error> errors = schema.validate(JacksonMigrationFunctions.toJackson3(parameters));

        if (!errors.isEmpty()) {
            final StringBuilder errorMessage = new StringBuilder("Invalid job parameters:");
            for (final Error error : errors) {
                errorMessage.append('\n').append(error.getMessage());
            }

            throw new BadRequestException(errorMessage.toString());
        }
    }

}
