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
package com.hpe.caf.services.job.api.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.Objects;

public final class LegacyJob extends Job
{

    private Date createTime = null;
    private Date lastUpdateTime = null;

    public LegacyJob(final Date createTime, final Date lastUpDateTime)
    {
        this.createTime = createTime;
        this.lastUpdateTime = lastUpDateTime;
    }

    public Job createTime(Date createTime)
    {
        this.createTime = createTime;
        return this;
    }

    @ApiModelProperty(value = "The time the job was created")
    @JsonProperty("createTime")
    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Job lastUpdateTime(Date lastUpdateTime)
    {
        this.lastUpdateTime = lastUpdateTime;
        return this;
    }

    @ApiModelProperty(value = "The time the job status or progress last changed")
    public Date getLastUpdateTime()
    {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime)
    {
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LegacyJob job = (LegacyJob) o;
        return Objects.equals(super.getId(), job.getId())
            && Objects.equals(super.getName(), job.getName())
            && Objects.equals(super.getDescription(), job.getDescription())
            && Objects.equals(super.getExternalData(), job.getExternalData())
            && Objects.equals(createTime, job.createTime)
            && Objects.equals(this.lastUpdateTime, job.lastUpdateTime)
            && Objects.equals(super.getStatus(), job.getStatus())
            && Objects.equals(super.getPercentageComplete(), job.getPercentageComplete())
            && Objects.equals(super.getFailures(), job.getFailures());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.getId(), super.getName(), super.getDescription(), super.getExternalData(), createTime, lastUpdateTime,
                            super.getStatus(), super.getPercentageComplete(), super.getFailures());
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("class Job {\n");
        sb.append("    id: ").append(toIndentedString(getId())).append("\n");
        sb.append("    name: ").append(toIndentedString(getName())).append("\n");
        sb.append("    description: ").append(toIndentedString(getDescription())).append("\n");
        sb.append("    externalData: ").append(toIndentedString(getExternalData())).append("\n");
        sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
        sb.append("    lastUpdateTime: ").append(toIndentedString(lastUpdateTime)).append("\n");
        sb.append("    status: ").append(toIndentedString(getStatus())).append("\n");
        sb.append("    percentageComplete: ").append(toIndentedString(getPercentageComplete())).append("\n");
        sb.append("    failures: ").append(toIndentedString(getFailures())).append("\n");
        sb.append("    labels: ").append(toIndentedString(getLabels())).append("\n");
        sb.append("}");
        return sb.toString();
    }
}





