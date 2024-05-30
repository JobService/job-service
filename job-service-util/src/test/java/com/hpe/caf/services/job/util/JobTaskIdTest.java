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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JobTaskIdTest {

    @Test
    public void testMessageId() {
        final String messageId = new JobTaskId("the partition-id", "the job-id").getMessageId();
        final JobTaskId id = JobTaskId.fromMessageId(messageId);
        Assertions.assertEquals("the partition-id", id.getPartitionId(), "should preserve partition ID");
        Assertions.assertEquals( "the job-id", id.getId(), "should preserve job ID");
    }

    @Test
    public void testFromOldFormatMessageId() {
        final JobTaskId id = JobTaskId.fromMessageId("the job-id");
        Assertions.assertEquals(JobTaskId.DEFAULT_PARTITION_ID, id.getPartitionId(), "should use default partition");
        Assertions.assertEquals("the job-id", id.getId(), "should preserve job ID");
    }
}
