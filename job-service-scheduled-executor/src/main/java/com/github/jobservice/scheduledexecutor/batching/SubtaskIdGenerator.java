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

/**
 * Generates subtask IDs for batched messages
 * <p>
 * Subtask IDs follow the format: {baseId}.{subtaskNumber} or {baseId}.{subtaskNumber}* for the final subtask.
 * The asterisk (*) marker on the final subtask is critical for progress calculation.
 */
public final class SubtaskIdGenerator
{
    private SubtaskIdGenerator()
    {
    }

    /**
     * Generates a subtask ID for a TaskMessage.
     * @param baseTaskId The base task UUID
     * @param subtaskIndex The 1-based index of this subtask
     * @param totalSubtasks The total number of subtasks
     * @return The subtask ID
     */
    public static String generateTaskSubtaskId(
        final String baseTaskId,
        final int subtaskIndex,
        final int totalSubtasks)
    {
        validateInputs(subtaskIndex, totalSubtasks);
        return formatSubtaskId(baseTaskId, subtaskIndex, subtaskIndex == totalSubtasks);
    }

    /**
     * Generates a subtask ID for TrackingInfo.jobTaskId.
     * @param baseJobTaskId The base job task ID
     * @param subtaskIndex The 1-based index of this subtask
     * @param totalSubtasks The total number of subtasks
     * @return The subtask job task ID
     */
    public static String generateJobTaskSubtaskId(
        final String baseJobTaskId,
        final int subtaskIndex,
        final int totalSubtasks)
    {
        validateInputs(subtaskIndex, totalSubtasks);
        return formatSubtaskId(baseJobTaskId, subtaskIndex, subtaskIndex == totalSubtasks);
    }

    /**
     * Formats the subtask ID with optional final marker.
     */
    private static String formatSubtaskId(
        final String baseId,
        final int subtaskIndex,
        final boolean isFinal)
    {
        final StringBuilder builder = new StringBuilder(baseId);
        builder.append('.');
        builder.append(subtaskIndex);
        if (isFinal) {
            builder.append('*');
        }
        return builder.toString();
    }

    /**
     * Validates subtask index inputs.
     */
    private static void validateInputs(final int subtaskIndex, final int totalSubtasks)
    {
        if (subtaskIndex < 1) {
            throw new IllegalArgumentException("Subtask index must be >= 1, got: " + subtaskIndex);
        }
        if (totalSubtasks < 1) {
            throw new IllegalArgumentException("Total subtasks must be >= 1, got: " + totalSubtasks);
        }
        if (subtaskIndex > totalSubtasks) {
            throw new IllegalArgumentException(
                "Subtask index (" + subtaskIndex + ") cannot exceed total (" + totalSubtasks + ")");
        }
    }
}

