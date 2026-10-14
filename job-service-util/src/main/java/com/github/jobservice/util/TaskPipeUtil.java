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

/**
 * Utility class for task pipe handling.
 * <p>
 * The "DocumentWorkerSubdocumentBatcher()" prefix is a directive that indicates
 * a job should be evaluated for payload batching. This prefix should be stripped
 * before sending messages to workers, as the actual target queue is the part
 * after the prefix.
 */
public final class TaskPipeUtil
{

    public static final String SUBDOCUMENT_BATCHER_PREFIX = "DocumentWorkerSubdocumentBatcher() ";

    private TaskPipeUtil()
    {
    }

    /**
     * Checks if the task pipe starts with the DocumentWorkerSubdocumentBatcher() prefix.
     *
     * @param taskPipe The task pipe to check
     * @return true if task pipe has the batching prefix, false otherwise
     */
    public static boolean hasSubdocumentBatcherPrefix(final String taskPipe)
    {
        return taskPipe != null && taskPipe.startsWith(SUBDOCUMENT_BATCHER_PREFIX);
    }

    /**
     * Strips the DocumentWorkerSubdocumentBatcher() prefix from the task pipe if present.
     * <p>
     * This method returns the actual target queue name that should be used for
     * publishing messages. The prefix is only a directive for the job service,
     * not part of the actual queue name.
     *
     * @param taskPipe The task pipe (may or may not have the prefix)
     * @return The task pipe without the prefix, or the original task pipe if prefix not present
     */
    public static String stripBatcherPrefix(final String taskPipe)
    {
        if (taskPipe != null && taskPipe.startsWith(SUBDOCUMENT_BATCHER_PREFIX)) {
            return taskPipe.substring(SUBDOCUMENT_BATCHER_PREFIX.length());
        }
        return taskPipe;
    }
}

