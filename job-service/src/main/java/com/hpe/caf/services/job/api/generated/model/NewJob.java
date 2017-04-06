/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-03-03T15:07:30.523Z")
@XmlRootElement(name = "NewJob")
public class NewJob   {

    private String name = null;
    private String description = null;
    private String externalData = null;
    private WorkerAction task = null;


    /**
     * The name of the job
     **/
    public NewJob name(String name) {
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
    public NewJob description(String description) {
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
    public NewJob externalData(String externalData) {
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
     **/
    public NewJob task(WorkerAction task) {
        this.task = task;
        return this;
    }


    @ApiModelProperty(value = "")
    @JsonProperty("task")
    public WorkerAction getTask() {
        return task;
    }
    public void setTask(WorkerAction task) {
        this.task = task;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NewJob newJob = (NewJob) o;
        return Objects.equals(name, newJob.name) &&
                Objects.equals(description, newJob.description) &&
                Objects.equals(externalData, newJob.externalData) &&
                Objects.equals(task, newJob.task);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, externalData, task);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class NewJob {\n");

        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    externalData: ").append(toIndentedString(externalData)).append("\n");
        sb.append("    task: ").append(toIndentedString(task)).append("\n");
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

