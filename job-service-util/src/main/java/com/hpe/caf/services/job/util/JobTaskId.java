/*
 * Copyright 2016-2024 Open Text.
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
package com.hpe.caf.services.job.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Uniquely identify a task (job or subtask).
 */
public final class JobTaskId {
    public static final String DEFAULT_PARTITION_ID = "default";

    private final String partitionId;
    private final String taskId;
    private final String jobId;
    private final String subtaskIds; // full suffix, eg. '.1.23.4'

    /**
     * @param partitionId Container for the job
     * @param taskId Unique task ID within the partition
     */
    public JobTaskId(final String partitionId, final String taskId) {
        this.partitionId = partitionId;
        this.taskId = taskId;

        final String[] parts = taskId.split("\\.", 2);
        if (parts.length == 2) {
            jobId = parts[0];
            subtaskIds = "." + parts[1];
        } else {
            jobId = taskId;
            subtaskIds = "";
        }
    }

    /**
     * Build from a job ID stored in a worker message, as in previous or current versions of
     * job-service.
     *
     * @param messageId Job ID from a worker message
     * @return parsed job identifier
     * @see #getMessageId
     */
    public static JobTaskId fromMessageId(final String messageId) {
        final String[] parts = messageId.split(":", 2);
        if (parts.length == 2) {
            return new JobTaskId(parts[0], parts[1]);
        } else {
            return new JobTaskId(DEFAULT_PARTITION_ID, messageId);
        }
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getId() {
        return taskId;
    }

    /**
     * @return unique identifier for use in a worker message.
     */
    public String getMessageId() {
        return partitionId + ":" + taskId;
    }
}
