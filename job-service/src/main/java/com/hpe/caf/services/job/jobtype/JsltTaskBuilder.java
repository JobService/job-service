/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import com.schibsted.spt.data.jslt.filters.TrueJsonFilter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Constructs task using a JSLT script. See the `Job-Types.md` document for a specification.
 *
 * @see com.schibsted.spt.data.jslt
 */
final class JsltTaskBuilder implements TaskBuilder {
    /**
     * Used for building script input.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String jobTypeId;
    private final Map<String, String> configuration;
    private final Map<String, String> constants;
    private final ParametersValidator parametersValidator;
    /**
     * Compiled script.
     */
    private final Expression script;

    /**
     * @param jobTypeId Job type's ID, used in error messages
     * @param configuration object containing string values which are fixed for the job type;
     *                      generally obtained from global configuration
     * @param constants object containing string values which are fixed for the job type
     * @param parametersValidator used to validate the `parameters` argument to {@link #build}
     *                            before passing them to the script
     * @param taskScript the uncompiled JSLT script
     * @throws InvalidJobTypeDefinitionException
     */
    public JsltTaskBuilder(
        final String jobTypeId,
        final Map<String, String> configuration,
        final Map<String, String> constants,
        final ParametersValidator parametersValidator,
        final String taskScript
    ) throws InvalidJobTypeDefinitionException {
        this.jobTypeId = jobTypeId;
        this.configuration = configuration;
        this.constants = constants;
        this.parametersValidator = parametersValidator;

        try {
            script = new Parser(new StringReader(taskScript))
                .withSource(jobTypeId)
                .withObjectFilter(new TrueJsonFilter())
                .compile();
        } catch (final JsltException e) {
            throw new InvalidJobTypeDefinitionException(
                jobTypeId + ": invalid taskDataScript", e);
        }
    }

    @Override
    public JsonNode build(final String partitionId, final String jobId, final JsonNode parameters)
        throws InvalidJobTypeDefinitionException, BadRequestException
    {
        parametersValidator.validate(parameters);

        final Map<String, Object> input = new HashMap<>();
        input.put("configuration", configuration);
        input.put("constants", constants);
        input.put("partitionId", partitionId);
        input.put("jobId", jobId);
        input.put("parameters", parameters);
        // should never fail
        final JsonNode inputJson = objectMapper.convertValue(input, JsonNode.class);

        try {
            return script.apply(inputJson);
        } catch (final JsltException e) {
            // an explicitly thrown error is distinguished by its message prefix and lack of
            // location information
            if (e.getSource() == null && e.getMessage().startsWith("error: ")) {
                throw new BadRequestException(jobTypeId + ": " + e.getMessage());
            } else {
                throw new InvalidJobTypeDefinitionException(
                    jobTypeId + ": taskScript execution failed", e);
            }
        }
    }

}
