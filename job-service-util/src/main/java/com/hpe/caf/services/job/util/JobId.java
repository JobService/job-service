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
package com.hpe.caf.services.job.util;

/**
 * Uniquely identify a job.
 */
public final class JobId {
    public static final String DEFAULT_PARTITION_ID = "default";

    private final String partitionId;
    private final String jobId;

    /**
     * @param partitionId Container for the job
     * @param jobId Unique job ID within the partition
     */
    public JobId(final String partitionId, final String jobId) {
        this.partitionId = partitionId;
        this.jobId = jobId;
    }

    /**
     * Build from a job ID stored in a worker message, as in previous or current versions of
     * job-service.
     *
     * @param messageId Job ID from a worker message
     * @return parsed job identifier
     * @see #getMessageId
     */
    public static JobId fromMessageId(final String messageId) {
        final String[] parts = messageId.split(":", 2);
        if (parts.length == 2) {
            return new JobId(parts[0], parts[1]);
        } else {
            return new JobId(DEFAULT_PARTITION_ID, messageId);
        }
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getId() {
        return jobId;
    }

    /**
     * @return unique identifier for use in a worker message.
     */
    public String getMessageId() {
        return partitionId + ":" + jobId;
    }

}
