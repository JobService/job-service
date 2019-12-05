/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
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

import org.junit.Assert;
import org.junit.Test;

public class JobTaskIdTest {

    @Test
    public void testMessageId() {
        final String messageId = new JobTaskId("the partition-id", "the job-id").getMessageId();
        final JobTaskId id = JobTaskId.fromMessageId(messageId);
        Assert.assertEquals("should preserve partition ID", "the partition-id", id.getPartitionId());
        Assert.assertEquals("should preserve job ID", "the job-id", id.getId());
    }

    @Test
    public void testFromOldFormatMessageId() {
        final JobTaskId id = JobTaskId.fromMessageId("the job-id");
        Assert.assertEquals("should use default partition",
            JobTaskId.DEFAULT_PARTITION_ID, id.getPartitionId());
        Assert.assertEquals("should preserve job ID", "the job-id", id.getId());
    }

    @Test
    public void testShortId() {
        final JobTaskId id = new JobTaskId("the partition-id", "the job-id");
        Assert.assertEquals("should generate short ID without invalid characters",
            "jtt8Lk2mSQc37bw//1qYWOeSHZrv0Doux2824zv62AA=",
            id.getShortId());
        Assert.assertNotEquals("different partition IDs should given different results",
            id.getShortId(), new JobTaskId("the partition-id-other", "the job-id").getShortId());
        Assert.assertNotEquals("different job IDs should given different results",
            id.getShortId(),
            new JobTaskId("the partition-id", "the job-id-other").getShortId());
    }

    @Test
    public void testShortIdWithSubtask() {
        final JobTaskId id = new JobTaskId("the partition-id", "the job-id.74");
        Assert.assertEquals("short ID should preserve subtask suffix",
            "jtt8Lk2mSQc37bw//1qYWOeSHZrv0Doux2824zv62AA=.74",
            id.getShortId());
    }

    @Test
    public void testShortIdWithNestedSubtask() {
        final JobTaskId id = new JobTaskId("the partition-id", "the job-id.74.1.13");
        Assert.assertEquals("short ID should preserve subtask suffix",
            "jtt8Lk2mSQc37bw//1qYWOeSHZrv0Doux2824zv62AA=.74.1.13",
            id.getShortId());
    }

}
