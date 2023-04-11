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
package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.worker.WorkerConfiguration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Configuration for the JobTrackingWorker.
 */
public class JobTrackingWorkerConfiguration extends WorkerConfiguration {

    /**
     * Output queue to return results to RabbitMQ.
     */
    @NotNull
    @Size(min = 1)
    private String outputQueue;

    /**
     * Number of threads to use in the worker.
     */
    @Min(1)
    @Max(20)
    private int threads;


    public JobTrackingWorkerConfiguration() { }


    public String getOutputQueue() {
        return outputQueue;
    }


    public void setOutputQueue(String outputQueue) {
        this.outputQueue = outputQueue;
    }


    public int getThreads() {
        return threads;
    }


    public void setThreads(int threads) {
        this.threads = threads;
    }
}
