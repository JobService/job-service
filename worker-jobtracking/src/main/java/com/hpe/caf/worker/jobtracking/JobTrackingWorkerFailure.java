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
package com.hpe.caf.worker.jobtracking;

import java.util.Date;

public class JobTrackingWorkerFailure   {

    private String failureId = null;
    private Date failureTime = null;
    private String failureSource = null;
    private String failureMessage = null;

    public JobTrackingWorkerFailure() {}

    public String getFailureId() {
        return failureId;
    }
    public void setFailureId(String failureId) {
        this.failureId = failureId;
    }

    public Date getFailureTime() {
        return failureTime;
    }
    public void setFailureTime(Date failureTime) {
        this.failureTime = failureTime;
    }

    public String getFailureSource() {
        return failureSource;
    }
    public void setFailureSource(String failureSource) {
        this.failureSource = failureSource;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

}

