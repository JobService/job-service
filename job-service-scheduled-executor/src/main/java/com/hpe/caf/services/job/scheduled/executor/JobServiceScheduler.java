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

import com.hpe.caf.worker.document.exceptions.DocumentWorkerTransientException;
import com.hpe.caf.worker.document.extensibility.DocumentWorker;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.HealthMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobServiceScheduler implements DocumentWorker
{
    private static final Logger LOG = LoggerFactory.getLogger(JobServiceScheduler.class);
    public JobServiceScheduler()
    {
        ScheduledExecutor.main(null);
    }

    /**
     * This method provides an opportunity for the worker to report if it has any problems which would prevent it processing documents
     * correctly. If the worker is healthy then it should simply return without calling the health monitor.
     *
     * @param healthMonitor used to report the health of the application
     */
    @Override
    public void checkHealth(HealthMonitor healthMonitor)
    {
        LOG.info("get health check");
    }

    @Override
    public void processDocument(Document document) throws InterruptedException, DocumentWorkerTransientException
    {
        LOG.info("start processing document");
        ScheduledExecutor.runAvailableJobs("Manual");
        LOG.info("finish processing document");
    }
}
