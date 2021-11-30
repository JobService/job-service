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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;

/**
 * A job type: defines a way of transforming a job in a specific format to a job in the normal,
 * unrestricted format, which performs a specific task.
 */
public final class JobType {
    /**
     * Used to convert the built output to the expected task format.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String id;
    private final TaskBuilder taskBuilder;

    /**
     * @param id Identifier for the job type which is unique within a {@link JobTypes} instance
     * @param taskBuilder How to construct the final job task
     */
    public JobType(
        final String id,
        final TaskBuilder taskBuilder
    ) {
        this.id = id;
        this.taskBuilder = taskBuilder;
    }

    /**
     * The job type identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @param partitionId The job's partition ID, provided to {@link TaskBuilder} as input
     * @param jobId The job's ID, provided to {@link TaskBuilder} as input
     * @param parameters Configuration submitted along with the job
     * @return Job task built according to the job type
     * @throws BadRequestException When the parameters are invalid
     * @throws InvalidJobTypeDefinitionException When the job type definition is invalid in a way
     *                                           that couldn't be determined at load-time
     */
    public WorkerAction buildTask(
        final String partitionId, final String jobId, final JsonNode parameters
    ) throws BadRequestException, InvalidJobTypeDefinitionException
    {
        final JsonNode task = taskBuilder.build(partitionId, jobId, parameters);
        JsonSchemaTaskScriptValidator.getInstance().validate(task);
        try {
            return objectMapper.treeToValue(task, WorkerAction.class);
        } catch (JsonProcessingException e) {
            // Should never happen, since we've already validated the task against the schema above.
            throw new InvalidJobTypeDefinitionException(id + ": unable to convert taskScript to WorkerAction", e);
        }
    }
}
