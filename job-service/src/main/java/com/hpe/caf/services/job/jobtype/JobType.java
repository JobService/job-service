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
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;

import java.util.Map;

/**
 * A job type: defines a way of transforming a job in a specific format to a job in the normal,
 * unrestricted format, which performs a specific task.
 */
public final class JobType {
    /**
     * Used to convert the built output to the expected task data format.
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String id;
    private final String taskClassifier;
    private final int taskApiVersion;
    private final String taskPipe;
    private final String targetPipe;
    private final TaskDataBuilder taskDataBuilder;

    /**
     * @param id Identifier for the job type which is unique within a {@link JobTypes} instance
     * @param taskClassifier Value to use for `taskClassifier` in the transformed job
     * @param taskApiVersion Value to use for `taskApiVersion` in the transformed job
     * @param taskPipe Value to use for `taskPipe` in the transformed job
     * @param targetPipe Value to use for `targetPipe` in the transformed job
     * @param taskDataBuilder How to construct the final job task data
     */
    public JobType(
        final String id,
        final String taskClassifier,
        final int taskApiVersion,
        final String taskPipe,
        final String targetPipe,
        final TaskDataBuilder taskDataBuilder
    ) {
        this.id = id;
        this.taskClassifier = taskClassifier;
        this.taskApiVersion = taskApiVersion;
        this.taskPipe = taskPipe;
        this.targetPipe = targetPipe;
        this.taskDataBuilder = taskDataBuilder;
    }

    /**
     * The job type identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @param partitionId The job's partition ID, provided to {@link TaskDataBuilder} as input
     * @param jobId The job's ID, provided to {@link TaskDataBuilder} as input
     * @param parameters Configuration submitted along with the job
     * @return Job task built according to the job type
     * @throws BadRequestException When the parameters are invalid
     * @throws InvalidJobTypeDefinitionException When the job type definition is invalid in a way
     *                                           that couldn't be determined at load-time
     */
    public WorkerAction buildTask(
        final String partitionId, final String jobId, final JsonNode parameters
    ) throws BadRequestException, InvalidJobTypeDefinitionException {
        final WorkerAction task = new WorkerAction();
        task.setTaskClassifier(taskClassifier);
        task.setTaskApiVersion(taskApiVersion);
        task.setTaskPipe(taskPipe);
        task.setTargetPipe(targetPipe);
        final JsonNode taskData = taskDataBuilder.build(partitionId, jobId, parameters);
        // job-put expects `Map` (or `String`), but only does a shallow type check
        try {
            task.setTaskData(objectMapper.convertValue(taskData, Map.class));
        } catch (final IllegalArgumentException e) {
            throw new InvalidJobTypeDefinitionException(
                id + ": incorrect output type for taskDataScript", e);
        }
        return task;
    }

}
