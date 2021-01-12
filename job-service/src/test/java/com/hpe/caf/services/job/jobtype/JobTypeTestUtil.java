/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JobTypeTestUtil {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param json valid JSON string
     * @return parsed JSON
     */
    public static JsonNode buildJson(final String json) {
        try {
            return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), JsonNode.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param object
     * @return JSON representation of object
     */
    public static JsonNode convertJson(final Object object) {
        return objectMapper.convertValue(object, JsonNode.class);
    }

    public final static JobType testJobType1 = new JobType(
        "id 1", "classifier 1", 101, "task pipe 1", "target pipe 1",
        (partitionId, jobId, params) -> buildJson("{ \"inside\": \"task data 1\" }"));
    public final static JobType testJobType2 = new JobType(
        "id 2", "classifier 2", 102, "task pipe 2", "target pipe 2",
        (partitionId, jobId, params) -> buildJson("{ \"inside\": \"task data 2\" }"));

}
