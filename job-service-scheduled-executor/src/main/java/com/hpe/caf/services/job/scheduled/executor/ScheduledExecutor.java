/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledExecutor.class);

    private static ScheduledFuture<?> future;

    public static void main(String[] args)
    {
        // Create a scheduler with one thread to process the pollDatabaseForJobsToRun() scheduled task.
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        LOG.info("Starting Job Service Scheduled Executor service ...");
        Runnable task = () -> {
            try {
                DatabasePoller.pollDatabaseForJobsToRun();
            } catch ( Throwable t ) {   // Catch Exceptions and Errors to prevent scheduler stoppage.
                LOG.error("Caught exception while polling the Job Service database. Message:\n" + t.getMessage()
                        + "StackTrace:\n" + Arrays.toString(t.getStackTrace()));
            }
        };

        //  Poll the Job Service database using the specified polling period configuration to specify how often the
        //  scheduled task is run.
        scheduler.scheduleWithFixedDelay(task, 0, ScheduledExecutorConfig.getScheduledExecutorPeriod(),
                TimeUnit.SECONDS);
    }
}
