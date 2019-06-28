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
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import com.schibsted.spt.data.jslt.filters.TrueJsonFilter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Constructs task data using a JSLT script.  See the `Job-Types.md` document for a specification.
 *
 * @see com.schibsted.spt.data.jslt
 */
final class JsltTaskDataBuilder implements TaskDataBuilder {
    /**
     * Used for building script input.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String jobTypeId;
    private final String taskPipe;
    private final String targetPipe;
    private final Map<String, String> configuration;
    private final ParametersValidator parametersValidator;
    /**
     * Compiled script.
     */
    private final Expression script;

    /**
     * @param jobTypeId Job type's ID, used in error messages
     * @param taskPipe The job's `taskPipe`
     * @param targetPipe The job's `targetPipe`; may be null
     * @param configuration object containing string values which are fixed for the job type;
     *                      generally obtained from global configuration
     * @param parametersValidator used to validate the `parameters` argument to {@link #build}
     *                            before passing them to the script
     * @param taskDataScript the uncompiled JSLT script
     * @throws InvalidJobTypeDefinitionException
     */
    public JsltTaskDataBuilder(
        final String jobTypeId,
        final String taskPipe,
        final String targetPipe,
        final Map<String, String> configuration,
        final ParametersValidator parametersValidator,
        final String taskDataScript
    ) throws InvalidJobTypeDefinitionException {
        this.jobTypeId = jobTypeId;
        this.taskPipe = taskPipe;
        this.targetPipe = targetPipe;
        this.configuration = configuration;
        this.parametersValidator = parametersValidator;

        try {
            script = new Parser(new StringReader(taskDataScript))
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
        input.put("taskPipe", taskPipe);
        if (targetPipe != null) input.put("targetPipe", targetPipe);
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
                    jobTypeId + ": taskDataScript execution failed", e);
            }
        }
    }

}
