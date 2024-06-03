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
package com.hpe.caf.services.job.jobtype;

import com.fasterxml.jackson.databind.node.TextNode;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobTypeTest {

    @BeforeEach
    public void loadTaskScriptSchema() {
        TaskScriptSchemaInitialization.initialize();
    }

    @Test
    public void testGetId() {
        assertEquals("id 1", JobTypeTestUtil.testJobType1.getId());
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

        assertEquals("classifier", task.getTaskClassifier(), "classifier should be as provided");
        assertEquals(123, task.getTaskApiVersion().intValue(), "api version should be as provided");
        assertEquals("task pipe", task.getTaskPipe(), "task pipe should be as provided");
        assertEquals("target pipe", task.getTargetPipe(), "target pipe should be as provided");

        final Map<String, Object> expectedTaskData = new HashMap<>();
        expectedTaskData.put("partitionId", "partition id");
        expectedTaskData.put("jobId", "job id");
        expectedTaskData.put("parameters", "params");
        assertEquals(expectedTaskData, task.getTaskData(), "task data should be as returned by provided builder");
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testBuildTaskWithInvalidParams() throws Exception {
        final JobType jobType = new JobType(
            "id",
            (partitionId, jobId, params) -> { throw new BadRequestException("invalid params"); });
        Assertions.assertThrows(BadRequestException.class, () -> jobType.buildTask("partition id", "job id", TextNode.valueOf("params")));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testBuildTaskWithInvalidTask() throws Exception {
        final JobType jobType = new JobType(
            "id",
            (partitionId, jobId, params) -> TextNode.valueOf("should be object"));
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> jobType.buildTask("partition id", "job id", TextNode.valueOf("params")));
    }
}
