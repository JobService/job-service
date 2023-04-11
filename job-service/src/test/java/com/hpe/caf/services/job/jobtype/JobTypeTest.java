/*
 * Copyright 2016-2023 Open Text.
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
import org.junit.Before;

public class JobTypeTest {

    @Before
    public void loadTaskScriptSchema() {
        new TaskScriptSchemaContextListener().contextInitialized(null);
    }

    @Test
    public void testGetId() {
        Assert.assertEquals("id 1", JobTypeTestUtil.testJobType1.getId());
    }

    @Test
    public void testBuildTask() throws Exception {
        final JobType jobType = new JobType(
            "id",
            (partitionId, jobId, params) -> {
                final HashMap<String, Object> taskData = new HashMap<>();
                taskData.put("partitionId", partitionId);
                taskData.put("jobId", jobId);
                taskData.put("parameters", params);
                
                final Map<String, Object> result = new HashMap<>();
                result.put("taskClassifier", "classifier");
                result.put("taskApiVersion", 123);
                result.put("taskData", taskData);
                result.put("taskPipe", "task pipe");
                result.put("targetPipe", "target pipe");
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

    @Test(expected = BadRequestException.class)
    public void testBuildTaskWithInvalidParams() throws Exception {
        final JobType jobType = new JobType(
            "id",
            (partitionId, jobId, params) -> { throw new BadRequestException("invalid params"); });
        jobType.buildTask("partition id", "job id", TextNode.valueOf("params"));
    }

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testBuildTaskWithInvalidTask() throws Exception {
        final JobType jobType = new JobType(
            "id",
            (partitionId, jobId, params) -> TextNode.valueOf("should be object"));
        jobType.buildTask("partition id", "job id", TextNode.valueOf("params"));
    }
}
