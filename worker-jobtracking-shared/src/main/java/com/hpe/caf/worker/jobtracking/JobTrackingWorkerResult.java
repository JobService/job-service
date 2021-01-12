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
package com.hpe.caf.worker.jobtracking;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Currently the worker framework requires every worker to issue a result message.
 * In the case of the Job Tracking worker the result message has no value so is effectively a dummy message.
 * It will never be consumed and will eventually time out.
 * This will remain the case until CAF-601 "Enhance Worker Framework to support Datagram MEP" is addressed.
 */
public class JobTrackingWorkerResult {
    /**
     * Worker-specific return code.
     */
    @NotNull
    private JobTrackingWorkerStatus status;

    /**
     * Return message.
     */
    private String message;


    public JobTrackingWorkerStatus getStatus() {
        return status;
    }


    public void setStatus(JobTrackingWorkerStatus status) {
        this.status = status;
    }


    public JobTrackingWorkerResult() {
    }


    public JobTrackingWorkerResult(JobTrackingWorkerStatus status) {
        this.status = Objects.requireNonNull(status);
    }


    public JobTrackingWorkerResult(JobTrackingWorkerStatus status, final String message) {
        this.status = Objects.requireNonNull(status);
        this.message = message;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(final String message) {
        this.message = message;
    }
}
