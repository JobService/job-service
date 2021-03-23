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

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class NoneLoaderTest {

    @Test
    public void testLoad() {
        Assert.assertEquals(Collections.emptyList(), new NoneLoader().load());
    }

    @Test
    public void testLoadTwice() throws Exception {
        final Loader loader = new NoneLoader();
        Assert.assertEquals(Collections.emptyList(), loader.load());
        Assert.assertEquals(Collections.emptyList(), loader.load());
    }

}
