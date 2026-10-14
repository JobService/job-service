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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for subdocuments extraction and batching.
 * <p>
 * This class uses index-based processing to avoid creating copies of large
 * subdocument arrays. Instead of splitting the entire list upfront, it provides
 * methods to get batch ranges and create batched payloads one at a time.
 */
public final class SubdocumentBatchSplitter
{
    /** JSON key for the document object in taskData */
    public static final String DOCUMENT_KEY = "document";

    /** JSON key for the subdocuments array within document */
    public static final String SUBDOCUMENTS_KEY = "subdocuments";

    private SubdocumentBatchSplitter()
    {
    }

    /**
     * Extracts the subdocuments list from a taskData map.
     * @param taskData The taskData map (deserialized from JSON)
     * @return The subdocuments list, or null if not found or not a list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> extractSubdocuments(final Map<String, Object> taskData)
    {
        if (taskData == null) {
            return null;
        }

        final Object documentObj = taskData.get(DOCUMENT_KEY);
        if (!(documentObj instanceof Map)) {
            return null;
        }

        final Map<String, Object> document = (Map<String, Object>) documentObj;
        final Object subdocsObj = document.get(SUBDOCUMENTS_KEY);

        if (!(subdocsObj instanceof List)) {
            return null;
        }

        return (List<Object>) subdocsObj;
    }

    /**
     * Calculates the number of batches needed for a given item count and batch size.
     * @param totalItems Total number of items
     * @param batchSize Maximum items per batch
     * @return Number of batches required
     */
    public static int calculateBatchCount(final int totalItems, final int batchSize)
    {
        if (totalItems <= 0) {
            return 0;
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be >= 1");
        }
        return Math.ceilDiv(totalItems,batchSize);
    }

    /**
     * Gets the start index (inclusive) for a specific batch.
     * @param batchIndex 1-based batch index
     * @param batchSize Items per batch
     * @return Start index (0-based, inclusive)
     */
    public static int getBatchStartIndex(final int batchIndex, final int batchSize)
    {
        if (batchIndex < 1) {
            throw new IllegalArgumentException("Batch index must be >= 1");
        }
        return (batchIndex - 1) * batchSize;
    }

    /**
     * Gets the end index (exclusive) for a specific batch.
     * @param batchIndex 1-based batch index
     * @param batchSize Items per batch
     * @param totalItems Total number of items
     * @return End index (0-based, exclusive)
     */
    public static int getBatchEndIndex(final int batchIndex, final int batchSize, final int totalItems)
    {
        final int start = getBatchStartIndex(batchIndex, batchSize);
        return Math.min(start + batchSize, totalItems);
    }

    /**
     * Gets the subdocuments for a specific batch as a subList VIEW.
     * @param subdocuments The full subdocuments list
     * @param batchIndex 1-based batch index
     * @param batchSize Items per batch
     * @return SubList view of subdocuments for this batch
     */
    public static List<Object> getSubdocumentsBatchView(
        final List<Object> subdocuments,
        final int batchIndex,
        final int batchSize)
    {
        final int start = getBatchStartIndex(batchIndex, batchSize);
        final int end = getBatchEndIndex(batchIndex, batchSize, subdocuments.size());
        return subdocuments.subList(start, end);
    }

    /**
     * Creates a reusable taskData template with document.subdocuments removed.
     * This avoids re-copying unchanged top-level and document entries for every batch.
     *
     * @param originalTaskData The original taskData map
     * @return A new template map without document.subdocuments
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createTaskDataTemplateWithoutSubdocuments(
        final Map<String, Object> originalTaskData)
    {
        final Map<String, Object> taskDataTemplate = new HashMap<>(originalTaskData);
        final Map<String, Object> originalDocument = (Map<String, Object>) originalTaskData.get(DOCUMENT_KEY);

        if (originalDocument != null) {
            final Map<String, Object> documentTemplate = new HashMap<>(originalDocument);
            documentTemplate.remove(SUBDOCUMENTS_KEY);
            taskDataTemplate.put(DOCUMENT_KEY, documentTemplate);
        }

        return taskDataTemplate;
    }

    /**
     * Creates a new taskData map from a reusable template and batch subdocuments.
     *
     * @param taskDataTemplate A taskData template created without document.subdocuments
     * @param subdocumentsBatch The batch of subdocuments to use (can be a subList view)
     * @return New taskData map with batch subdocuments added into document
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createBatchedTaskDataFromTemplate(
        final Map<String, Object> taskDataTemplate,
        final List<Object> subdocumentsBatch)
    {
        final Map<String, Object> batchedTaskData = new HashMap<>(taskDataTemplate);
        final Map<String, Object> documentTemplate = (Map<String, Object>) taskDataTemplate.get(DOCUMENT_KEY);

        if (documentTemplate != null) {
            final Map<String, Object> batchedDocument = new HashMap<>(documentTemplate);
            batchedDocument.put(SUBDOCUMENTS_KEY, subdocumentsBatch);
            batchedTaskData.put(DOCUMENT_KEY, batchedDocument);
        }

        return batchedTaskData;
    }
}

