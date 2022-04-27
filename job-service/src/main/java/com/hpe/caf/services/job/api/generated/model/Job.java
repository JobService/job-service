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
package com.hpe.caf.services.job.api.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-03-03T15:07:30.523Z")
@XmlRootElement(name = "Job")
public class Job   {

    private String id = null;
    private String name = null;
    private String description = null;
    /**
     * @deprecated 21/01/2020 - Replaced by labels functionality.
     */
    @Deprecated
    private String externalData = null;
    private Date createTime = null;
    private Date lastUpdateTime = null;


    public enum StatusEnum {
        ACTIVE("Active"),
        CANCELLED("Cancelled"),
        COMPLETED("Completed"),
        FAILED("Failed"),
        PAUSED("Paused"),
        WAITING("Waiting");

        private String value;

        StatusEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }
    }

    private StatusEnum status = null;
    private Float percentageComplete = null;
    private List<Failure> failures = new ArrayList<Failure>();
    private Map<String, String> labels = new HashMap<>();

    /**
     * The job identifier
     **/
    public Job id(String id) {
        this.id = id;
        return this;
    }


    @ApiModelProperty(value = "The job identifier")
    @JsonProperty("id")
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }


    /**
     * The name of the job
     **/
    public Job name(String name) {
        this.name = name;
        return this;
    }


    @ApiModelProperty(value = "The name of the job")
    @JsonProperty("name")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }


    /**
     * The description of the job
     **/
    public Job description(String description) {
        this.description = description;
        return this;
    }


    @ApiModelProperty(value = "The description of the job")
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * External data can be associated with the job for use by other components
     **/
    public Job externalData(String externalData) {
        this.externalData = externalData;
        return this;
    }


    @ApiModelProperty(value = "External data can be associated with the job for use by other components")
    @JsonProperty("externalData")
    public String getExternalData() {
        return externalData;
    }
    public void setExternalData(String externalData) {
        this.externalData = externalData;
    }


    /**
     * The time the job was created
     **/
    public Job createTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }


    @ApiModelProperty(value = "The time the job was created")
    @JsonProperty("createTime")
    public Date getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * The time the job status or progress last changed
     **/
    public Job lastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
        return this;
    }

    @ApiModelProperty(value = "The time the job status or progress last changed")
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * The status of the job
     **/
    public Job status(StatusEnum status) {
        this.status = status;
        return this;
    }


    @ApiModelProperty(value = "The status of the job")
    @JsonProperty("status")
    public StatusEnum getStatus() {
        return status;
    }
    public void setStatus(StatusEnum status) {
        this.status = status;
    }


    /**
     * Gives an indication of the progress of the job
     **/
    public Job percentageComplete(Float percentageComplete) {
        this.percentageComplete = percentageComplete;
        return this;
    }


    @ApiModelProperty(value = "Gives an indication of the progress of the job")
    @JsonProperty("percentageComplete")
    public Float getPercentageComplete() {
        return percentageComplete;
    }
    public void setPercentageComplete(Float percentageComplete) {
        this.percentageComplete = percentageComplete;
    }


    /**
     * Job failure details
     **/
    public Job failures(List<Failure> failures) {
        this.failures = failures;
        return this;
    }


    @ApiModelProperty(value = "Job failure details")
    @JsonProperty("failures")
    public List<Failure> getFailures() {
        return failures;
    }
    public void setFailures(List<Failure> failures) {
        this.failures = failures;
    }

    @ApiModelProperty("Extra meta-data related to the job")
    @JsonProperty("labels")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(final Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Job job = (Job) o;
        return Objects.equals(id, job.id) &&
                Objects.equals(name, job.name) &&
                Objects.equals(description, job.description) &&
                Objects.equals(externalData, job.externalData) &&
                Objects.equals(createTime, job.createTime) &&
                Objects.equals(this.lastUpdateTime, job.lastUpdateTime) &&
                Objects.equals(status, job.status) &&
                Objects.equals(percentageComplete, job.percentageComplete) &&
                Objects.equals(failures, job.failures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, externalData, createTime, lastUpdateTime, status, percentageComplete, failures);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Job {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    externalData: ").append(toIndentedString(externalData)).append("\n");
        sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
        sb.append("    lastUpdateTime: ").append(toIndentedString(lastUpdateTime)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    percentageComplete: ").append(toIndentedString(percentageComplete)).append("\n");
        sb.append("    failures: ").append(toIndentedString(failures)).append("\n");
        sb.append("    labels: ").append(toIndentedString(labels)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
