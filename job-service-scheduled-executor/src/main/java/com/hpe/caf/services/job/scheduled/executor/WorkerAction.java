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

import java.util.Objects;

/**
 * This class is used to publish messages on to RabbitMQ.
 * NOTE - best to keep this in synch with
 * job-service\src\main\java\com\hpe\caf\services\job\api\generated\model\WorkerAction.java
 */
public class WorkerAction
{
    private String taskClassifier = null;
    private Integer taskApiVersion = null;
    private Object taskData = null;


    public enum TaskDataEncodingEnum {
        UTF8("utf8"),
        BASE64("base64");

        private String value;

        TaskDataEncodingEnum(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private TaskDataEncodingEnum taskDataEncoding = null;
    private String taskPipe = null;
    private String targetPipe = null;

    public WorkerAction taskClassifier(final String taskClassifier) {
        this.taskClassifier = taskClassifier;
        return this;
    }

    public String getTaskClassifier() {
        return taskClassifier;
    }
    public void setTaskClassifier(final String taskClassifier) {
        this.taskClassifier = taskClassifier;
    }

    public WorkerAction taskApiVersion(final Integer taskApiVersion) {
        this.taskApiVersion = taskApiVersion;
        return this;
    }

    public Integer getTaskApiVersion() {
        return taskApiVersion;
    }
    public void setTaskApiVersion(final Integer taskApiVersion) {
        this.taskApiVersion = taskApiVersion;
    }

    public WorkerAction taskData(final Object taskData) {
        this.taskData = taskData;
        return this;
    }

    public Object getTaskData() {
        return taskData;
    }
    public void setTaskData(final Object taskData) {
        this.taskData = taskData;
    }

    public WorkerAction taskDataEncoding(final TaskDataEncodingEnum taskDataEncoding) {
        this.taskDataEncoding = taskDataEncoding;
        return this;
    }

    public TaskDataEncodingEnum getTaskDataEncoding() {
        return taskDataEncoding;
    }
    public void setTaskDataEncoding(final TaskDataEncodingEnum taskDataEncoding) {
        this.taskDataEncoding = taskDataEncoding;
    }

    public WorkerAction taskPipe(final String taskPipe) {
        this.taskPipe = taskPipe;
        return this;
    }

    public String getTaskPipe() {
        return taskPipe;
    }
    public void setTaskPipe(final String taskPipe) {
        this.taskPipe = taskPipe;
    }

    public WorkerAction targetPipe(final String targetPipe) {
        this.targetPipe = targetPipe;
        return this;
    }

    public String getTargetPipe() {
        return targetPipe;
    }
    public void setTargetPipe(final String targetPipe) {
        this.targetPipe = targetPipe;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final WorkerAction workerAction = (WorkerAction) o;
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
        final StringBuilder sb = new StringBuilder();
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
    private String toIndentedString(final Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
