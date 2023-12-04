/*
 * Copyright 2016-2023 Open Text.
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
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@jakarta.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-03-03T15:07:30.523Z")
@XmlRootElement(name = "WorkerAction")
public class WorkerAction   {

    private String taskClassifier = null;
    private Integer taskApiVersion = null;
    private Object taskData = null;


    public enum TaskDataEncodingEnum {
        UTF8("utf8"),
        BASE64("base64");

        private String value;

        TaskDataEncodingEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }
    }

    private TaskDataEncodingEnum taskDataEncoding = null;
    private String taskPipe = null;
    private String targetPipe = null;


    /**
     **/
    public WorkerAction taskClassifier(String taskClassifier) {
        this.taskClassifier = taskClassifier;
        return this;
    }


    @ApiModelProperty(required = true, value = "")
    @JsonProperty("taskClassifier")
    public String getTaskClassifier() {
        return taskClassifier;
    }
    public void setTaskClassifier(String taskClassifier) {
        this.taskClassifier = taskClassifier;
    }


    /**
     **/
    public WorkerAction taskApiVersion(Integer taskApiVersion) {
        this.taskApiVersion = taskApiVersion;
        return this;
    }


    @ApiModelProperty(required = true, value = "")
    @JsonProperty("taskApiVersion")
    public Integer getTaskApiVersion() {
        return taskApiVersion;
    }
    public void setTaskApiVersion(Integer taskApiVersion) {
        this.taskApiVersion = taskApiVersion;
    }


    /**
     **/
    public WorkerAction taskData(Object taskData) {
        this.taskData = taskData;
        return this;
    }


    @ApiModelProperty(required = true, value = "")
    @JsonProperty("taskData")
    public Object getTaskData() {
        return taskData;
    }
    public void setTaskData(Object taskData) {
        this.taskData = taskData;
    }


    /**
     **/
    public WorkerAction taskDataEncoding(TaskDataEncodingEnum taskDataEncoding) {
        this.taskDataEncoding = taskDataEncoding;
        return this;
    }


    @ApiModelProperty(value = "")
    @JsonProperty("taskDataEncoding")
    public TaskDataEncodingEnum getTaskDataEncoding() {
        return taskDataEncoding;
    }
    public void setTaskDataEncoding(TaskDataEncodingEnum taskDataEncoding) {
        this.taskDataEncoding = taskDataEncoding;
    }


    /**
     **/
    public WorkerAction taskPipe(String taskPipe) {
        this.taskPipe = taskPipe;
        return this;
    }


    @ApiModelProperty(required = true, value = "")
    @JsonProperty("taskPipe")
    public String getTaskPipe() {
        return taskPipe;
    }
    public void setTaskPipe(String taskPipe) {
        this.taskPipe = taskPipe;
    }


    /**
     **/
    public WorkerAction targetPipe(String targetPipe) {
        this.targetPipe = targetPipe;
        return this;
    }


    @ApiModelProperty(required = true, value = "")
    @JsonProperty("targetPipe")
    public String getTargetPipe() {
        return targetPipe;
    }
    public void setTargetPipe(String targetPipe) {
        this.targetPipe = targetPipe;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkerAction workerAction = (WorkerAction) o;
        return Objects.equals(taskClassifier, workerAction.taskClassifier) &&
                Objects.equals(taskApiVersion, workerAction.taskApiVersion) &&
                Objects.equals(taskData, workerAction.taskData) &&
                Objects.equals(taskDataEncoding, workerAction.taskDataEncoding) &&
                Objects.equals(taskPipe, workerAction.taskPipe) &&
                Objects.equals(targetPipe, workerAction.targetPipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskClassifier, taskApiVersion, taskData, taskDataEncoding, taskPipe, targetPipe);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class WorkerAction {\n");

        sb.append("    taskClassifier: ").append(toIndentedString(taskClassifier)).append("\n");
        sb.append("    taskApiVersion: ").append(toIndentedString(taskApiVersion)).append("\n");
        sb.append("    taskData: ").append(toIndentedString(taskData)).append("\n");
        sb.append("    taskDataEncoding: ").append(toIndentedString(taskDataEncoding)).append("\n");
        sb.append("    taskPipe: ").append(toIndentedString(taskPipe)).append("\n");
        sb.append("    targetPipe: ").append(toIndentedString(targetPipe)).append("\n");
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

