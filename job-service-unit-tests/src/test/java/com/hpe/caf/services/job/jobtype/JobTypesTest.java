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

import com.hpe.caf.services.job.exceptions.BadRequestException;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class JobTypesTest {

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testInitialiseWithDuplicateIds() throws Exception {
        Assertions.assertThrows(InvalidJobTypeDefinitionException.class, () -> JobTypes.initialise(() -> Arrays.asList(
            JobTypeTestUtil.testJobType1, JobTypeTestUtil.testJobType1
        )));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testGetWithNoTypes() throws Exception {
        JobTypes.initialise(() -> Collections.emptyList());
        Assertions.assertThrows(BadRequestException.class, () -> JobTypes.getInstance().getJobType("type"));
    }

    @Test
    public void testGetExistingType() throws Exception {
        JobTypes.initialise(() -> Collections.singletonList(JobTypeTestUtil.testJobType1));
        assertEquals(JobTypeTestUtil.testJobType1,  JobTypes.getInstance().getJobType("id 1"));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void testUnknownType() throws Exception {
        JobTypes.initialise(() -> Collections.singletonList(JobTypeTestUtil.testJobType1));
        Assertions.assertThrows(BadRequestException.class, () -> JobTypes.getInstance().getJobType("id 2"));
    }

    @Test
    public void testGetWithMultipleTypes() throws Exception {
        JobTypes.initialise(() -> Arrays.asList(
            JobTypeTestUtil.testJobType1, JobTypeTestUtil.testJobType2
        ));
        assertEquals(JobTypeTestUtil.testJobType1,
            JobTypes.getInstance().getJobType("id 1"));
        assertEquals(JobTypeTestUtil.testJobType2,
            JobTypes.getInstance().getJobType("id 2"));
    }

}
