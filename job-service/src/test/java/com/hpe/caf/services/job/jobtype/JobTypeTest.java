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
package com.hpe.caf.services.job.jobtype;

import com.fasterxml.jackson.databind.node.TextNode;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class JobTypeTest {

    @Test
    public void testGetId() {
        Assert.assertEquals("id 1", JobTypeTestUtil.testJobType1.getId());
    }

    @Test
    public void testBuildTask() throws Exception {
        final JobType jobType = new JobType(
            "id", "classifier", 123, "task pipe", "target pipe",
            (partitionId, jobId, params) -> {
                final Map<String, Object> result = new HashMap<>();
                result.put("partitionId", partitionId);
                result.put("jobId", jobId);
                result.put("parameters", params);
                return JobTypeTestUtil.convertJson(result);
            });

        final WorkerAction task =
            jobType.buildTask("partition id", "job id", TextNode.valueOf("params"));

        Assert.assertEquals("classifier should be as provided",
            "classifier", task.getTaskClassifier());
        Assert.assertEquals("api version should be as provided",
            123, task.getTaskApiVersion().intValue());
        Assert.assertEquals("task pipe should be as provided",
            "task pipe", task.getTaskPipe());
        Assert.assertEquals("target pipe should be as provided",
            "target pipe", task.getTargetPipe());

        final Map<String, Object> expectedTaskData = new HashMap<>();
        expectedTaskData.put("partitionId", "partition id");
        expectedTaskData.put("jobId", "job id");
        expectedTaskData.put("parameters", "params");
        Assert.assertEquals("task data should be as returned by provided builder",
            expectedTaskData, task.getTaskData());
    }

    @Test
    public void testBuildTaskWithNullTargetPipe() throws Exception {
        final JobType jobType = new JobType(
            "id", "classifier", 123, "task pipe", null,
            (partitionId, jobId, params) -> {
                return JobTypeTestUtil.convertJson(new HashMap<String, String>());
            });
        final WorkerAction task =
            jobType.buildTask("partition id", "job id", TextNode.valueOf("params"));
        Assert.assertNull("target pipe in task should null", task.getTargetPipe());
    }

    @Test(expected = BadRequestException.class)
    public void testBuildTaskWithInvalidParams() throws Exception {
        final JobType jobType = new JobType(
            "id", "classifier", 123, "task pipe", "target pipe",
            (partitionId, jobId, params) -> { throw new BadRequestException("invalid params"); });
        jobType.buildTask("partition id", "job id", TextNode.valueOf("params"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testBuildTaskWithInvalidTaskData() throws Exception {
        final JobType jobType = new JobType(
            "id", "classifier", 123, "task pipe", "target pipe",
            (partitionId, jobId, params) -> TextNode.valueOf("should be object"));
        jobType.buildTask("partition id", "job id", TextNode.valueOf("params"));
    }

}
