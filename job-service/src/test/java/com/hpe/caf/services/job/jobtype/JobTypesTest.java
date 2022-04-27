/*
 * Copyright 2016-2022 Micro Focus or one of its affiliates.
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
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class JobTypesTest {

    @Test(expected = InvalidJobTypeDefinitionException.class)
    public void testInitialiseWithDuplicateIds() throws Exception {
        JobTypes.initialise(() -> Arrays.asList(
            JobTypeTestUtil.testJobType1, JobTypeTestUtil.testJobType1
        ));
    }

    @Test(expected = BadRequestException.class)
    public void testGetWithNoTypes() throws Exception {
        JobTypes.initialise(() -> Collections.emptyList());
        JobTypes.getInstance().getJobType("type");
    }

    @Test
    public void testGetExistingType() throws Exception {
        JobTypes.initialise(() -> Collections.singletonList(JobTypeTestUtil.testJobType1));
        Assert.assertEquals(JobTypeTestUtil.testJobType1,
            JobTypes.getInstance().getJobType("id 1"));
    }

    @Test(expected = BadRequestException.class)
    public void testUnknownType() throws Exception {
        JobTypes.initialise(() -> Collections.singletonList(JobTypeTestUtil.testJobType1));
        JobTypes.getInstance().getJobType("id 2");
    }

    @Test
    public void testGetWithMultipleTypes() throws Exception {
        JobTypes.initialise(() -> Arrays.asList(
            JobTypeTestUtil.testJobType1, JobTypeTestUtil.testJobType2
        ));
        Assert.assertEquals(JobTypeTestUtil.testJobType1,
            JobTypes.getInstance().getJobType("id 1"));
        Assert.assertEquals(JobTypeTestUtil.testJobType2,
            JobTypes.getInstance().getJobType("id 2"));
    }

}
