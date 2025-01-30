/*
 * Copyright 2016-2025 Open Text.
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
package com.github.jobservice.scheduledexecutor;

import com.github.cafdataprocessing.workers.document.exceptions.DocumentWorkerTransientException;
import com.github.cafdataprocessing.workers.document.extensibility.DocumentWorker;
import com.github.cafdataprocessing.workers.document.model.Document;
import com.github.cafdataprocessing.workers.document.model.HealthMonitor;

public class JobServiceScheduler implements DocumentWorker
{
    private final ScheduledExecutor scheduler;

    public JobServiceScheduler()
    {
        this.scheduler = new ScheduledExecutor();
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
    }

    @Override
    public void processDocument(Document document) throws InterruptedException, DocumentWorkerTransientException
    {
        scheduler.poke();
    }
}
