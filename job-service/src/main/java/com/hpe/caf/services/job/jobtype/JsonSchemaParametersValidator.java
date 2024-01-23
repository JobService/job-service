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
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import java.util.Set;

/**
 * Validate parameters using JSON Schema.
 */
final class JsonSchemaParametersValidator implements ParametersValidator {

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
    {
        final VersionFlag schemaVersion = SpecVersionDetector.detectOptionalVersion(schema).orElse(VersionFlag.V4);
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(schemaVersion);
        this.schema = factory.getSchema(schema);
    }

    @Override
    public void validate(final JsonNode parameters) throws BadRequestException {
        final Set<ValidationMessage> errors = schema.validate(parameters);

        if (!errors.isEmpty()) {
            final StringBuilder errorMessage = new StringBuilder("Invalid job parameters:");
            for (final ValidationMessage error : errors) {
                errorMessage.append('\n').append(error.getMessage());
            }

            throw new BadRequestException(errorMessage.toString());
        }
    }

}
