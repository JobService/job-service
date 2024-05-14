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
package com.hpe.caf.services.job.scheduled.executor;

public final class StagingQueueRerouter {
    private StagingQueueRerouter() {}

    private static final String LOAD_BALANCED_INDICATOR = "»";

    /**
     * Create a staging queue name from the 
     * @param targetQueue The original queue name
     * @param tenant The tenant id
     * @return A new queue name that combines the original queue name with the tenant id
     */
    public static String route(final String targetQueue, final String tenant) {
        return targetQueue + LOAD_BALANCED_INDICATOR + "/" + tenant;
    }
}
