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
package com.github.jobservice.scheduledexecutor.batching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jobservice.scheduledexecutor.WorkerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for detecting when payload batching is needed.
 * <p>
 * This service implements the payload batching strategy for DocumentWorkerTask jobs
 * with large subdocuments arrays, splitting them into smaller batches.
 * <p>
 * Batching is opt-in: only jobs whose task pipe starts with the
 * {@code DocumentWorkerSubdocumentBatcher()} prefix will be batched.
 */
public final class PayloadBatchingService
{
    private static final Logger LOG = LoggerFactory.getLogger(PayloadBatchingService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};

    /** Task classifier that triggers batching */
    public static final String DOCUMENT_WORKER_TASK_CLASSIFIER = "DocumentWorkerTask";

    /** Task pipe prefix that opts-in to subdocument batching */
    public static final String SUBDOCUMENT_BATCHER_PREFIX = "DocumentWorkerSubdocumentBatcher() ";

    /** Maximum number of subdocuments per batch */
    public static final int BATCH_SIZE = 200;

    private PayloadBatchingService()
    {
    }

    /**
     * Determines whether a worker action's payload should be batched.
     * <p>
     * Batching is triggered only when ALL of these conditions are met:
     * <ul>
     *   <li>taskClassifier == "DocumentWorkerTask"</li>
     *   <li>task pipe starts with "DocumentWorkerSubdocumentBatcher() "</li>
     *   <li>payload has document.subdocuments array</li>
     *   <li>subdocuments array size > BATCH_SIZE</li>
     * </ul>
     *
     * @param workerAction The worker action to evaluate
     * @return true if the payload should be batched, false otherwise
     */
    public static boolean shouldBatchPayload(final WorkerAction workerAction)
    {
        if (!DOCUMENT_WORKER_TASK_CLASSIFIER.equals(workerAction.getTaskClassifier())) {
            LOG.debug("Not a DocumentWorkerTask, skipping batching");
            return false;
        }

        if (!hasSubdocumentBatcherPrefix(workerAction)) {
            LOG.debug("Task pipe does not have DocumentWorkerSubdocumentBatcher() prefix, skipping batching");
            return false;
        }

        final int subdocCount = getSubdocumentsCount(workerAction);
        if (subdocCount < 0) {
            LOG.debug("Could not determine subdocuments count, skipping batching");
            return false;
        }

        if (subdocCount <= BATCH_SIZE) {
            LOG.debug("Subdocuments count ({}) <= batch size ({}), skipping batching",
                      subdocCount, BATCH_SIZE);
            return false;
        }

        LOG.info("Payload batching required: {} subdocuments will be split into batches of {}",
                 subdocCount, BATCH_SIZE);
        return true;
    }

    /**
     * Checks if the task pipe starts with the DocumentWorkerSubdocumentBatcher() prefix.
     * @param workerAction The worker action
     * @return true if task pipe has the batching prefix, false otherwise
     */
    public static boolean hasSubdocumentBatcherPrefix(final WorkerAction workerAction)
    {
        final String taskPipe = workerAction.getTaskPipe();
        return taskPipe != null && taskPipe.startsWith(SUBDOCUMENT_BATCHER_PREFIX);
    }

    /**
     * Strips the DocumentWorkerSubdocumentBatcher() prefix from the task pipe.
     * @param workerAction The worker action
     * @return The task pipe without the prefix, or the original task pipe if prefix not present
     */
    public static String stripBatcherPrefix(final WorkerAction workerAction)
    {
        final String taskPipe = workerAction.getTaskPipe();
        if (taskPipe != null && taskPipe.startsWith(SUBDOCUMENT_BATCHER_PREFIX)) {
            return taskPipe.substring(SUBDOCUMENT_BATCHER_PREFIX.length());
        }
        return taskPipe;
    }

    /**
     * Gets the subdocuments count from a worker action.
     * @param workerAction The worker action
     * @return Subdocuments count, or -1 if cannot be determined
     */
    public static int getSubdocumentsCount(final WorkerAction workerAction)
    {
        final Map<String, Object> taskDataMap = deserializeTaskData(workerAction);
        if (taskDataMap == null) {
            return -1;
        }

        final List<Object> subdocuments = SubdocumentBatchSplitter.extractSubdocuments(taskDataMap);
        if (subdocuments == null) {
            return -1;
        }

        return subdocuments.size();
    }

    /**
     * Deserializes the taskData JSON String to a Map.
     * @param workerAction The worker action containing taskData
     * @return Deserialized Map, or null if deserialization fails
     */
    public static Map<String, Object> deserializeTaskData(final WorkerAction workerAction)
    {
        final Object taskDataObj = workerAction.getTaskData();

        if (taskDataObj == null) {
            LOG.debug("TaskData is null");
            return null;
        }

        if (!(taskDataObj instanceof String)) {
            LOG.debug("TaskData is not a String, got: {}", taskDataObj.getClass().getName());
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue((String) taskDataObj, MAP_TYPE_REF);
        } catch (final JsonProcessingException e) {
            LOG.debug("Failed to deserialize taskData as JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculates the total number of batches needed.
     * @param workerAction The worker action
     * @return Number of batches, or 0 if batching not applicable
     */
    public static int calculateTotalBatches(final WorkerAction workerAction)
    {
        final int subdocCount = getSubdocumentsCount(workerAction);
        if (subdocCount <= 0) {
            return 0;
        }
        return SubdocumentBatchSplitter.calculateBatchCount(subdocCount, BATCH_SIZE);
    }

    /**
     * Gets the batch size constant.
     * @return The batch size (200)
     */
    public static int getBatchSize()
    {
        return BATCH_SIZE;
    }
}

