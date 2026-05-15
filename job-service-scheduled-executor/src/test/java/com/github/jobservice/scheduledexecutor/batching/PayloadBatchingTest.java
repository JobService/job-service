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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jobservice.scheduledexecutor.WorkerAction;
import com.github.jobservice.util.TaskPipeUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for payload batching classes: PayloadBatchingService, SubdocumentBatchSplitter, SubtaskIdGenerator.
 */
public class PayloadBatchingTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int BATCH_SIZE = PayloadBatchingService.BATCH_SIZE;
    private static final String PARTITION_ID = "tenant-user1";
    private static final String JOB_ID = "job123";
    private static final String TARGET_PIPE = "target-pipe";
    private static final String BASE_TASK_PIPE = "worker-queue";
    private static final String TASK_PIPE_WITH_PREFIX = TaskPipeUtil.SUBDOCUMENT_BATCHER_PREFIX + BASE_TASK_PIPE;

    // =====================================================
    // DETECTION TESTS (PayloadBatchingService)
    // Opt-in via task pipe prefix: DocumentWorkerSubdocumentBatcher()
    // =====================================================

    @Test
    public void testDocumentWorkerTask_WithPrefix_NoSubdocuments_NoSplitting() throws JsonProcessingException
    {
        // DocumentWorkerTask with prefix but without subdocuments field
        final WorkerAction action = createWorkerActionWithPrefix(createTaskDataWithoutSubdocs());
        assertFalse(PayloadBatchingService.shouldBatchPayload(action));
    }

    @Test
    public void testDocumentWorkerTask_WithPrefix_EmptySubdocuments_NoSplitting() throws JsonProcessingException
    {
        // DocumentWorkerTask with prefix and empty subdocuments array
        final WorkerAction action = createWorkerActionWithPrefix(createTaskDataMap(0));
        assertFalse(PayloadBatchingService.shouldBatchPayload(action));
    }

    @Test
    public void testDocumentWorkerTask_WithPrefix_SmallSubdocuments_NoSplitting() throws JsonProcessingException
    {
        // DocumentWorkerTask with prefix and subdocuments below threshold
        final WorkerAction action = createWorkerActionWithPrefix(createTaskDataMap(BATCH_SIZE / 2));
        assertFalse(PayloadBatchingService.shouldBatchPayload(action));
    }

    @Test
    public void testDocumentWorkerTask_WithPrefix_LargeSubdocuments_SplittingEnabled() throws JsonProcessingException
    {
        // DocumentWorkerTask with prefix and subdocuments above threshold
        final WorkerAction action = createWorkerActionWithPrefix(createTaskDataMap(BATCH_SIZE + 1));
        assertTrue(PayloadBatchingService.shouldBatchPayload(action));
    }

//    @Test
//    public void testDocumentWorkerTask_NoPrefix_LargeSubdocuments_NoSplitting() throws JsonProcessingException
//    {
//        // DocumentWorkerTask WITHOUT prefix but with large subdocuments (not opted in)
//        final WorkerAction action = createWorkerActionWithoutPrefix(createTaskDataMap(BATCH_SIZE * 5));
//        assertFalse(PayloadBatchingService.shouldBatchPayload(action));
//    }

    @Test
    public void testNonDocumentWorkerTask_WithPrefix_NoSplitting() throws JsonProcessingException
    {
        // Non-DocumentWorkerTask with prefix in task pipe (wrong classifier)
        final WorkerAction action = createWorkerActionWithPrefix(createTaskDataMap(BATCH_SIZE * 5));
        action.setTaskClassifier("NonDocumentWorkerTask");
        assertFalse(PayloadBatchingService.shouldBatchPayload(action));
    }

    @Test
    public void testShouldBatch_NullTaskData_ReturnsFalse()
    {
        final WorkerAction action = new WorkerAction();
        action.setTaskClassifier(PayloadBatchingService.DOCUMENT_WORKER_TASK_CLASSIFIER);
        action.setTaskPipe(TASK_PIPE_WITH_PREFIX);
        action.setTaskData(null);
        assertFalse(PayloadBatchingService.shouldBatchPayload(action));
    }

    @Test
    public void testGetSubdocumentsCount_Valid() throws JsonProcessingException
    {
        final WorkerAction action = createWorkerActionWithPrefix(createTaskDataMap(500));
        assertEquals(500, PayloadBatchingService.getSubdocumentsCount(action));
    }

    @Test
    public void testCalculateTotalBatches() throws JsonProcessingException
    {
        final WorkerAction action = createWorkerActionWithPrefix(createTaskDataMap(500));
        assertEquals(3, PayloadBatchingService.calculateTotalBatches(action)); // 500/200 = 2.5 = 3
    }

    @Test
    public void testDeserializeTaskData_Valid() throws JsonProcessingException
    {
        final WorkerAction action = createWorkerActionWithPrefix(createTaskDataMap(10));
        final Map<String, Object> result = PayloadBatchingService.deserializeTaskData(action);
        assertNotNull(result);
        assertTrue(result.containsKey("document"));
    }

    @Test
    public void testDeserializeTaskData_InvalidJson()
    {
        final WorkerAction action = new WorkerAction();
        action.setTaskClassifier(PayloadBatchingService.DOCUMENT_WORKER_TASK_CLASSIFIER);
        action.setTaskPipe(TASK_PIPE_WITH_PREFIX);
        action.setTaskData("invalid json {{{");
        assertNull(PayloadBatchingService.deserializeTaskData(action));
    }

    // =====================================================
    // SPLITTING LOGIC TESTS (SubdocumentBatchSplitter)
    // =====================================================

    @Test
    public void testCalculateBatchCount_ExactDivision()
    {
        assertEquals(10, SubdocumentBatchSplitter.calculateBatchCount(1000, 100));
    }

    @Test
    public void testCalculateBatchCount_UnevenDivision()
    {
        assertEquals(11, SubdocumentBatchSplitter.calculateBatchCount(1050, 100));
    }

    @Test
    public void testCalculateBatchCount_JustOverThreshold()
    {
        assertEquals(2, SubdocumentBatchSplitter.calculateBatchCount(BATCH_SIZE + 1, BATCH_SIZE));
    }

    @Test
    public void testCalculateBatchCount_ZeroItems()
    {
        assertEquals(0, SubdocumentBatchSplitter.calculateBatchCount(0, 100));
    }

    @Test
    public void testCalculateBatchCount_InvalidBatchSize()
    {
        assertThrows(IllegalArgumentException.class,
            () -> SubdocumentBatchSplitter.calculateBatchCount(100, 0));
    }

    @Test
    public void testGetBatchStartIndex()
    {
        assertEquals(0, SubdocumentBatchSplitter.getBatchStartIndex(1, 100));
        assertEquals(100, SubdocumentBatchSplitter.getBatchStartIndex(2, 100));
        assertEquals(200, SubdocumentBatchSplitter.getBatchStartIndex(3, 100));
    }

    @Test
    public void testGetBatchStartIndex_InvalidIndex()
    {
        assertThrows(IllegalArgumentException.class,
            () -> SubdocumentBatchSplitter.getBatchStartIndex(0, 100));
    }

    @Test
    public void testGetSubdocumentsBatchView_PreservesOrder()
    {
        final List<Object> subdocs = createSubdocList(500);
        final int batchSize = 100;

        final List<Object> batch1 = SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, 1, batchSize);
        final List<Object> batch3 = SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, 3, batchSize);

        assertEquals("1", getSubdocId(batch1.get(0)));
        assertEquals("100", getSubdocId(batch1.get(99)));
        assertEquals("201", getSubdocId(batch3.get(0)));
        assertEquals("300", getSubdocId(batch3.get(99)));
    }

    @Test
    public void testGetSubdocumentsBatchView_LastBatch()
    {
        final List<Object> subdocs = createSubdocList(250);
        final int batchSize = 100;

        final List<Object> lastBatch = SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, 3, batchSize);
        assertEquals(50, lastBatch.size());
        assertEquals("201", getSubdocId(lastBatch.get(0)));
        assertEquals("250", getSubdocId(lastBatch.get(49)));
    }

    @Test
    public void testExtractSubdocuments_MissingDocument()
    {
        final Map<String, Object> taskData = new HashMap<>();
        taskData.put("someField", "value");
        assertNull(SubdocumentBatchSplitter.extractSubdocuments(taskData));
    }

    @Test
    public void testExtractSubdocuments_MissingSubdocuments()
    {
        final Map<String, Object> taskData = new HashMap<>();
        taskData.put("document", new HashMap<>());
        assertNull(SubdocumentBatchSplitter.extractSubdocuments(taskData));
    }

    // =====================================================
    // PAYLOAD INTEGRITY TESTS
    // =====================================================

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateBatchedTaskData_PreservesOtherFields()
    {
        final Map<String, Object> original = createTaskDataMap(500);
        final List<Object> subdocs = SubdocumentBatchSplitter.extractSubdocuments(original);

        for (int i = 1; i <= 3; i++) {
            final List<Object> batch = SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, i, BATCH_SIZE);
            final Map<String, Object> batched = SubdocumentBatchSplitter.createBatchedTaskData(original, batch);

            // Verify customData and scripts preserved
            assertEquals(original.get("customData"), batched.get("customData"));
            assertEquals(original.get("scripts"), batched.get("scripts"));
            
            // Verify document fields are preserved in each batch
            final Map<String, Object> originalDoc = (Map<String, Object>) original.get("document");
            final Map<String, Object> batchedDoc = (Map<String, Object>) batched.get("document");
            assertEquals(originalDoc.get("fields"), batchedDoc.get("fields"), 
                "Document fields should be preserved in batch " + i);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateBatchedTaskData_CorrectSubdocumentsInEachBatch()
    {
        final Map<String, Object> original = createTaskDataMap(500);
        final List<Object> subdocs = SubdocumentBatchSplitter.extractSubdocuments(original);

        final List<Object> batch1 = SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, 1, BATCH_SIZE);
        final List<Object> batch2 = SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, 2, BATCH_SIZE);
        final List<Object> batch3 = SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, 3, BATCH_SIZE);

        assertEquals(BATCH_SIZE, batch1.size());
        assertEquals(BATCH_SIZE, batch2.size());
        assertEquals(100, batch3.size());

        assertEquals("1", getSubdocId(batch1.get(0)));
        assertEquals("200", getSubdocId(batch1.get(BATCH_SIZE - 1)));
        assertEquals("201", getSubdocId(batch2.get(0)));
        assertEquals("400", getSubdocId(batch2.get(BATCH_SIZE - 1)));
        assertEquals("401", getSubdocId(batch3.get(0)));
        assertEquals("500", getSubdocId(batch3.get(batch3.size() - 1)));
    }

    @Test
    public void testBatching_NoSubdocumentDuplication()
    {
        final List<Object> subdocs = createSubdocList(500);
        final Set<String> seen = new HashSet<>();
        final int totalBatches = SubdocumentBatchSplitter.calculateBatchCount(500, BATCH_SIZE);

        for (int i = 1; i <= totalBatches; i++) {
            for (final Object item : SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, i, BATCH_SIZE)) {
                final String id = getSubdocId(item);
                assertFalse(seen.contains(id), "Duplicate: " + id);
                seen.add(id);
            }
        }
        assertEquals(500, seen.size());
    }

    @Test
    public void testBatching_NoSubdocumentLoss()
    {
        final List<Object> subdocs = createSubdocList(500);
        int total = 0;
        final int totalBatches = SubdocumentBatchSplitter.calculateBatchCount(500, BATCH_SIZE);

        for (int i = 1; i <= totalBatches; i++) {
            total += SubdocumentBatchSplitter.getSubdocumentsBatchView(subdocs, i, BATCH_SIZE).size();
        }
        assertEquals(500, total);
    }

    // =====================================================
    // SUBTASK ID TESTS (SubtaskIdGenerator)
    // =====================================================

    @Test
    public void testGenerateJobTaskSubtaskId_Format()
    {
        final String baseJobTaskId = PARTITION_ID + ":" + JOB_ID;
        assertEquals(PARTITION_ID + ":" + JOB_ID + ".1",
            SubtaskIdGenerator.generateJobTaskSubtaskId(baseJobTaskId, 1, 3));
        assertEquals(PARTITION_ID + ":" + JOB_ID + ".2",
            SubtaskIdGenerator.generateJobTaskSubtaskId(baseJobTaskId, 2, 3));
        assertEquals(PARTITION_ID + ":" + JOB_ID + ".3*",
            SubtaskIdGenerator.generateJobTaskSubtaskId(baseJobTaskId, 3, 3));
    }

    @Test
    public void testGenerateTaskSubtaskId_Format()
    {
        final String baseTaskId = "abc-123-def-456";
        assertEquals("abc-123-def-456.1", SubtaskIdGenerator.generateTaskSubtaskId(baseTaskId, 1, 3));
        assertEquals("abc-123-def-456.2", SubtaskIdGenerator.generateTaskSubtaskId(baseTaskId, 2, 3));
        assertEquals("abc-123-def-456.3*", SubtaskIdGenerator.generateTaskSubtaskId(baseTaskId, 3, 3));
    }

    @Test
    public void testSubtaskId_OnlyLastHasFinalMarker()
    {
        final String baseId = "base";
        for (int i = 1; i < 5; i++) {
            assertFalse(SubtaskIdGenerator.generateTaskSubtaskId(baseId, i, 5).endsWith("*"));
        }
        assertTrue(SubtaskIdGenerator.generateTaskSubtaskId(baseId, 5, 5).endsWith("*"));
    }

    @Test
    public void testSubtaskId_SingleBatch_HasFinalMarker()
    {
        assertEquals("base.1*", SubtaskIdGenerator.generateTaskSubtaskId("base", 1, 1));
    }

    @Test
    public void testSubtaskId_BothIdsHaveMatchingSuffix()
    {
        final String baseTaskId = "task-uuid";
        final String baseJobTaskId = "partition:job";

        for (int i = 1; i <= 3; i++) {
            final String taskSuffix = SubtaskIdGenerator.generateTaskSubtaskId(baseTaskId, i, 3)
                .substring(baseTaskId.length());
            final String jobTaskSuffix = SubtaskIdGenerator.generateJobTaskSubtaskId(baseJobTaskId, i, 3)
                .substring(baseJobTaskId.length());
            assertEquals(taskSuffix, jobTaskSuffix);
        }
    }

    @Test
    public void testSubtaskId_InvalidSubtaskIndex()
    {
        assertThrows(IllegalArgumentException.class,
            () -> SubtaskIdGenerator.generateTaskSubtaskId("base", 0, 3));
        assertThrows(IllegalArgumentException.class,
            () -> SubtaskIdGenerator.generateTaskSubtaskId("base", 4, 3));
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    /**
     * Creates a WorkerAction WITH the DocumentWorkerSubdocumentBatcher() prefix (opted-in for batching)
     */
    private WorkerAction createWorkerActionWithPrefix(final Map<String, Object> taskData) throws JsonProcessingException
    {
        final WorkerAction action = new WorkerAction();
        action.setTaskClassifier(PayloadBatchingService.DOCUMENT_WORKER_TASK_CLASSIFIER);
        action.setTaskApiVersion(2);
        action.setTaskData(OBJECT_MAPPER.writeValueAsString(taskData));
        action.setTaskPipe(TASK_PIPE_WITH_PREFIX);
        action.setTargetPipe(TARGET_PIPE);
        return action;
    }

    /**
     * Creates a WorkerAction WITHOUT the DocumentWorkerSubdocumentBatcher() prefix (not opted-in)
     */
    private WorkerAction createWorkerActionWithoutPrefix(final Map<String, Object> taskData) throws JsonProcessingException
    {
        final WorkerAction action = new WorkerAction();
        action.setTaskClassifier(PayloadBatchingService.DOCUMENT_WORKER_TASK_CLASSIFIER);
        action.setTaskApiVersion(2);
        action.setTaskData(OBJECT_MAPPER.writeValueAsString(taskData));
        action.setTaskPipe(BASE_TASK_PIPE);
        action.setTargetPipe(TARGET_PIPE);
        return action;
    }

    // Creates taskData with actual structure - numbering starts from 1
    private Map<String, Object> createTaskDataMap(final int subdocCount)
    {
        final Map<String, Object> taskData = new HashMap<>();

        final Map<String, Object> document = new HashMap<>();
        // Add fields inside document (like targetId, destinationId in real payloads)
        final Map<String, Object> documentFields = new HashMap<>();
        documentFields.put("targetId", List.of(Map.of("data", "2")));
        documentFields.put("destinationId", List.of(Map.of("data", "4")));
        document.put("fields", documentFields);
        document.put("subdocuments", createSubdocList(subdocCount));
        taskData.put("document", document);

        final Map<String, Object> customData = new HashMap<>();
        customData.put("tenantId", "testTenant");
        customData.put("workflowName", "test-workflow");
        customData.put("timestamp", "1774593207");
        taskData.put("customData", customData);

        taskData.put("scripts", new ArrayList<>());

        return taskData;
    }

    // Creates subdoc list with actual structure - numbering starts from 1
    private List<Object> createSubdocList(final int count)
    {
        final List<Object> subdocs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            final Map<String, Object> subdoc = new HashMap<>();
            final Map<String, Object> fields = new HashMap<>();
            fields.put("_id", List.of(Map.of("data", String.valueOf(i))));
            fields.put("_index", List.of(Map.of("data", "test-index")));
            fields.put("_routing", List.of(Map.of("data", String.valueOf(i))));
            subdoc.put("fields", fields);
            subdocs.add(subdoc);
        }
        return subdocs;
    }

    private Map<String, Object> createTaskDataWithoutSubdocs()
    {
        final Map<String, Object> taskData = new HashMap<>();
        taskData.put("document", new HashMap<>());
        taskData.put("customData", Map.of("key", "value"));
        taskData.put("scripts", new ArrayList<>());
        return taskData;
    }

    @SuppressWarnings("unchecked")
    private String getSubdocId(final Object subdoc)
    {
        final Map<String, Object> subdocMap = (Map<String, Object>) subdoc;
        final Map<String, Object> fields = (Map<String, Object>) subdocMap.get("fields");
        final List<Map<String, Object>> idList = (List<Map<String, Object>>) fields.get("_id");
        return (String) idList.get(0).get("data");
    }
}
