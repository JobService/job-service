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
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;

import java.util.Map;

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
        final WorkerAction workerAction = new WorkerAction();
        final JsonNode task = taskBuilder.build(partitionId, jobId, parameters);
        // job-put expects `Map` (or `String`), but only does a shallow type check
        try {
            // taskClassifier
            final JsonNode taskClassifier = getNonNullPropertyFromTask(task, "taskClassifier");
            if (!taskClassifier.isTextual() || taskClassifier.asText().isEmpty()) {
                throw new InvalidJobTypeDefinitionException(
                    id + ": taskScript should contain a non-empty string value for: taskClassifier");
            }
            workerAction.setTaskClassifier(taskClassifier.asText());

            // taskApiVersion
            final JsonNode taskApiVersion = getNonNullPropertyFromTask(task, "taskApiVersion");
            if (!taskApiVersion.isInt()) {
                throw new InvalidJobTypeDefinitionException(
                    id + ": taskScript should contain an integer value for: taskApiVersion");
            }
            workerAction.setTaskApiVersion(taskApiVersion.asInt());

            // taskData
            final JsonNode taskData = getNonNullPropertyFromTask(task, "taskData");
            if (!taskData.isObject()) {
                throw new InvalidJobTypeDefinitionException(
                    id + ": taskScript should contain an object value for: taskData");
            }
            workerAction.setTaskData(objectMapper.convertValue(taskData, Map.class));

            // taskPipe
            final JsonNode taskPipe = getNonNullPropertyFromTask(task, "taskPipe");
            if (!taskPipe.isTextual() || taskPipe.asText().isEmpty()) {
                throw new InvalidJobTypeDefinitionException(
                    id + ": taskScript should contain a non-empty string value for: taskPipe");
            }
            workerAction.setTaskPipe(taskPipe.asText());

            // targetPipe
            final JsonNode targetPipe = task.get("targetPipe");
            if (targetPipe != null && !targetPipe.isNull()) {
                if (!targetPipe.isTextual()) {
                    throw new InvalidJobTypeDefinitionException(
                        id + ": taskScript should contain a string value for: targetPipe (when it has been provided)");
                }
                workerAction.setTargetPipe(targetPipe.asText());
            }
        } catch (final IllegalArgumentException e) {
            throw new InvalidJobTypeDefinitionException(
                id + ": incorrect output type for taskScript", e);
        }
        return workerAction;
    }

    private JsonNode getNonNullPropertyFromTask(final JsonNode task, final String property)
        throws InvalidJobTypeDefinitionException
    {
        final JsonNode jsonNode = task.get(property);
        if (jsonNode == null || jsonNode.isNull()) {
            throw new InvalidJobTypeDefinitionException(
                id + ": taskScript should contain a non-null value for: " + property);
        }
        return jsonNode;
    }
}
