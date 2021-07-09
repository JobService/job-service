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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledExecutor.class);

    public static void main(final String[] args)
    {
        runJobs();
    }

    private static void runJobs() {
        // Create a scheduler to process scheduled tasks.
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        LOG.info("Starting Job Service Scheduled Executor service ...");
        
        final Runnable task = () -> runAvailableJobs("Auto");
    
        //  Poll the Job Service database using the specified polling period configuration to specify how often the
        //  scheduled task is run.
        scheduler.scheduleWithFixedDelay(task, 20, ScheduledExecutorConfig.getScheduledExecutorPeriod(),
                TimeUnit.SECONDS);

        LOG.info("Starting task for dropping soft deleted tables ...");
        //  Execute the dropTablesTask periodically.
        scheduler.scheduleWithFixedDelay(new DropTablesTask(), 20, ScheduledExecutorConfig.getDropTablesSchedulerPeriod(),
                TimeUnit.SECONDS);
    }
    
    /**
     *
     * @param origin the trigger's origin. It can be "Auto" or "Manual"
     */
    public static void runAvailableJobs(final String origin)
    {
        try {
            final Instant start = Instant.now();
            DatabasePoller.pollDatabaseForJobsToRun();
            final Instant end = Instant.now();
            LOG.debug("Total time taken to execute scheduler for {} task in ms {}", origin, Duration.between(start, end).toMillis());
        } catch (final Exception t ) {   // Catch Exceptions and Errors to prevent scheduler stoppage.
            LOG.error("Caught exception while polling the Job Service database. Message:\n{} StackTrace:\n{}",
                    t.getMessage(), Arrays.toString(t.getStackTrace()));
        }
    }
    
}
