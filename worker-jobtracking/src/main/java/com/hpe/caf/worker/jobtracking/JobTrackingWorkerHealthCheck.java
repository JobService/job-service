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
package com.hpe.caf.worker.jobtracking;

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check for the JobTrackingWorker.
 */
public class JobTrackingWorkerHealthCheck implements HealthReporter {

    private static final Logger LOG = LoggerFactory.getLogger(JobTrackingWorker.class);

    private final JobTrackingReporter reporter;


    public JobTrackingWorkerHealthCheck(JobTrackingReporter reporter) {
        this.reporter = reporter;
    }


    /**
     * The health check checks if all the external components that the worker depends on are available.
     */
    @Override
    public HealthResult healthCheck() {
        try
        {
            if (!reporter.verifyJobDatabase()) {
                LOG.warn("Error contacting Job Database.");
                return new HealthResult(HealthStatus.UNHEALTHY, "Job Database connection check failed.");
            }
            return HealthResult.RESULT_HEALTHY;
        } catch (Exception e) {
            LOG.warn("Error contacting Job Database. ", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Job Database connection check failed. " + e.getMessage());
        }
    }
}
