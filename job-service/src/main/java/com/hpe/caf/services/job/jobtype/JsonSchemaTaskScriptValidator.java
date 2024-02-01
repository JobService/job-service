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
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import java.util.Set;

public final class JsonSchemaTaskScriptValidator {

    private static JsonSchemaTaskScriptValidator INSTANCE;

    private final JsonSchema compiledTaskScriptSchema;

    private JsonSchemaTaskScriptValidator(final JsonNode taskScriptSchema)
    {
        final VersionFlag schemaVersion = SpecVersionDetector.detectOptionalVersion(taskScriptSchema, true).orElse(VersionFlag.V4);
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(schemaVersion);
        this.compiledTaskScriptSchema = factory.getSchema(taskScriptSchema);
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
        final Set<ValidationMessage> errors = compiledTaskScriptSchema.validate(taskScript);

        if (!errors.isEmpty()) {
            final StringBuilder errorMessage = new StringBuilder("Invalid taskScript:");
            for (final ValidationMessage error : errors) {
                errorMessage.append('\n').append(error.getMessage());
            }
            throw new InvalidJobTypeDefinitionException(errorMessage.toString());
        }
    }
}
