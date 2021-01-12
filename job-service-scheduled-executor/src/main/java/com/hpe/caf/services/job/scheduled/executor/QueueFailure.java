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
package com.hpe.caf.services.job.scheduled.executor;

import java.util.Date;
import java.util.Objects;

/**
 * This class is used to register a failure in publishing a message on to RabbitMQ.
 * NOTE - best to keep this in synch with
 * job-service\src\main\java\com\hpe\caf\services\job\api\generated\model\Failure.java
 */
public class QueueFailure
{
    private String failureId = null;
    private Date failureTime = null;
    private String failureSource = null;
    private String failureMessage = null;

    public QueueFailure failureId(final String failureId) {
        this.failureId = failureId;
        return this;
    }

    public String getFailureId() {
        return failureId;
    }
    public void setFailureId(final String failureId) {
        this.failureId = failureId;
    }

    public QueueFailure failureTime(final Date failureTime) {
        this.failureTime = failureTime;
        return this;
    }

    public Date getFailureTime() {
        return failureTime;
    }
    public void setFailureTime(final Date failureTime) {
        this.failureTime = failureTime;
    }

    public QueueFailure failureSource(final String failureSource) {
        this.failureSource = failureSource;
        return this;
    }

    public String getFailureSource() {
        return failureSource;
    }
    public void setFailureSource(final String failureSource) {
        this.failureSource = failureSource;
    }

    public QueueFailure failureMessage(final String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
    public void setFailureMessage(final String failureMessage) {
        this.failureMessage = failureMessage;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final QueueFailure failure = (QueueFailure) o;
        return Objects.equals(failureId, failure.failureId) &&
                Objects.equals(failureSource, failure.failureSource) &&
                Objects.equals(failureTime, failure.failureTime) &&
                Objects.equals(failureMessage, failure.failureMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(failureId, failureTime, failureSource, failureMessage);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("class Failure {\n");

        sb.append("    failureId: ").append(toIndentedString(failureId)).append("\n");
        sb.append("    failureSource: ").append(toIndentedString(failureSource)).append("\n");
        sb.append("    failureTime: ").append(toIndentedString(failureTime)).append("\n");
        sb.append("    failureMessage: ").append(toIndentedString(failureMessage)).append("\n");
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
