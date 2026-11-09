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
package com.github.jobservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskPipeUtil.
 */
public class TaskPipeUtilTest
{
    private static final String BASE_TASK_PIPE = "worker-queue";
    private static final String TASK_PIPE_WITH_PREFIX = TaskPipeUtil.SUBDOCUMENT_BATCHER_PREFIX + BASE_TASK_PIPE;

    // =====================================================
    // hasSubdocumentBatcherPrefix TESTS
    // =====================================================

    @Test
    public void testHasSubdocumentBatcherPrefix_WithPrefix()
    {
        assertTrue(TaskPipeUtil.hasSubdocumentBatcherPrefix(TASK_PIPE_WITH_PREFIX));
    }

    @Test
    public void testHasSubdocumentBatcherPrefix_WithoutPrefix()
    {
        assertFalse(TaskPipeUtil.hasSubdocumentBatcherPrefix(BASE_TASK_PIPE));
    }

    @Test
    public void testHasSubdocumentBatcherPrefix_NullTaskPipe()
    {
        assertFalse(TaskPipeUtil.hasSubdocumentBatcherPrefix(null));
    }

    @Test
    public void testHasSubdocumentBatcherPrefix_EmptyString()
    {
        assertFalse(TaskPipeUtil.hasSubdocumentBatcherPrefix(""));
    }

    @Test
    public void testHasSubdocumentBatcherPrefix_PartialPrefix()
    {
        // Should not match partial prefix
        assertFalse(TaskPipeUtil.hasSubdocumentBatcherPrefix("DocumentWorkerSubdocument"));
    }

    // =====================================================
    // stripBatcherPrefix TESTS
    // =====================================================

    @Test
    public void testStripBatcherPrefix_WithPrefix()
    {
        assertEquals(BASE_TASK_PIPE, TaskPipeUtil.stripBatcherPrefix(TASK_PIPE_WITH_PREFIX));
    }

    @Test
    public void testStripBatcherPrefix_WithoutPrefix()
    {
        assertEquals(BASE_TASK_PIPE, TaskPipeUtil.stripBatcherPrefix(BASE_TASK_PIPE));
    }

    @Test
    public void testStripBatcherPrefix_NullTaskPipe()
    {
        assertNull(TaskPipeUtil.stripBatcherPrefix(null));
    }

    @Test
    public void testStripBatcherPrefix_EmptyString()
    {
        assertEquals("", TaskPipeUtil.stripBatcherPrefix(""));
    }

    @Test
    public void testStripBatcherPrefix_OnlyPrefix()
    {
        // When task pipe is exactly the prefix, result should be empty
        assertEquals("", TaskPipeUtil.stripBatcherPrefix(TaskPipeUtil.SUBDOCUMENT_BATCHER_PREFIX));
    }

    @Test
    public void testStripBatcherPrefix_PrefixWithSpecialCharacters()
    {
        final String queueWithSpecialChars = "my-worker.queue/input";
        final String prefixed = TaskPipeUtil.SUBDOCUMENT_BATCHER_PREFIX + queueWithSpecialChars;
        assertEquals(queueWithSpecialChars, TaskPipeUtil.stripBatcherPrefix(prefixed));
    }

    // =====================================================
    // CONSTANT TESTS
    // =====================================================

    @Test
    public void testPrefixEndsWithSpace()
    {
        // The prefix should end with a space to separate from actual queue name
        assertTrue(TaskPipeUtil.SUBDOCUMENT_BATCHER_PREFIX.endsWith(" "));
    }

    @Test
    public void testPrefixValue()
    {
        assertEquals("DocumentWorkerSubdocumentBatcher() ", TaskPipeUtil.SUBDOCUMENT_BATCHER_PREFIX);
    }
}

