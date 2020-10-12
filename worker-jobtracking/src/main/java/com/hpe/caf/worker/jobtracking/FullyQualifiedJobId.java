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
package com.hpe.caf.worker.jobtracking;

import java.util.Objects;

final class FullyQualifiedJobId
{
    private final String partitionId;
    private final String jobId;

    public FullyQualifiedJobId(
        final String partitionId,
        final String jobId
    )
    {
        this.partitionId = Objects.requireNonNull(partitionId);
        this.jobId = Objects.requireNonNull(jobId);
    }

    public String getPartitionId()
    {
        return partitionId;
    }

    public String getJobId()
    {
        return jobId;
    }

    @Override
    public boolean equals(final Object obj)
    {
        // TODO: Check values of partition id and job id
    }

    @Override
    public int hashCode()
    {
        // TODO: Implement
    }
}
