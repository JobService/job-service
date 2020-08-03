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
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public final class CorrectedFormatJob extends Job
{
    private DateTime createTime = null;
    private DateTime lastUpdateTime = null;

    public CorrectedFormatJob(final String createTime, final String lastUpdateTime)
    {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        this.createTime = formatter.parseDateTime(createTime);
        this.lastUpdateTime = formatter.parseDateTime(lastUpdateTime);
    }
    
    public Job createTime(DateTime createTime)
    {
        this.createTime = createTime;
        return this;
    }

    @ApiModelProperty(value = "The time the job was created")
    @JsonProperty("createTime")
    public DateTime getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(DateTime createTime)
    {
        this.createTime = createTime;
    }

    public Job lastUpdateTime(DateTime lastUpdateTime)
    {
        this.lastUpdateTime = lastUpdateTime;
        return this;
    }

    @ApiModelProperty(value = "The time the job status or progress last changed")
    public DateTime getLastUpdateTime()
    {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(DateTime lastUpdateTime)
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
        final CorrectedFormatJob job = (CorrectedFormatJob) o;
        return Objects.equals(super.getId(), job.getId())
            && Objects.equals(super.getName(), job.getName())
            && Objects.equals(super.getDescription(), job.getDescription())
            && Objects.equals(super.getExternalData(), job.getExternalData())
            && Objects.equals(createTime, job.createTime)
            && Objects.equals(lastUpdateTime, job.lastUpdateTime)
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
        sb.append("    id: ").append(toIndentedString(super.getId())).append("\n");
        sb.append("    name: ").append(toIndentedString(super.getName())).append("\n");
        sb.append("    description: ").append(toIndentedString(super.getDescription())).append("\n");
        sb.append("    externalData: ").append(toIndentedString(super.getExternalData())).append("\n");
        sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
        sb.append("    lastUpdateTime: ").append(toIndentedString(lastUpdateTime)).append("\n");
        sb.append("    status: ").append(toIndentedString(super.getStatus())).append("\n");
        sb.append("    percentageComplete: ").append(toIndentedString(super.getPercentageComplete())).append("\n");
        sb.append("    failures: ").append(toIndentedString(super.getFailures())).append("\n");
        sb.append("    labels: ").append(toIndentedString(super.getLabels())).append("\n");
        sb.append("}");
        return sb.toString();
    }
}






