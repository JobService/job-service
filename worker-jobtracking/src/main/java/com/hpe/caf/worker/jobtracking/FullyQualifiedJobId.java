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

/**
 * FullyQualifiedJobId allows grouping by partition and jobId. It implements the comparable interface
 * that will allow an ordering by partition/job
 */
final class FullyQualifiedJobId implements Comparable<FullyQualifiedJobId>
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

    /**
     * @param that object to compare
     * @return 0 if equals, <0 if before, >0 if after
     */
    @Override
    public int compareTo(final FullyQualifiedJobId that)
    {
        final int partitionCompareResult = partitionId.compareTo(that.partitionId);

        return (partitionCompareResult == 0)
            ? jobId.compareTo(that.jobId)
            : partitionCompareResult;
    }

    /**
     *
     * @param obj object to be compared
     * @return true if equals (same partitionId and jobId), false otherwise
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FullyQualifiedJobId)) {
            return false;
        }
        final FullyQualifiedJobId that = (FullyQualifiedJobId) obj;
        return partitionId.equals(that.partitionId) && jobId.equals(that.jobId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(partitionId, jobId);
    }
}
