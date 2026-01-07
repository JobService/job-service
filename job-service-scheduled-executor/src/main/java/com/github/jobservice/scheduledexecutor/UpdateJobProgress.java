/*
 * Copyright 2016-2026 Open Text.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;

public final class UpdateJobProgress implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateJobProgress.class);

    private final int numOfTasksToUpdate;

    public UpdateJobProgress(int numOfTasksToUpdate) {
        this.numOfTasksToUpdate = numOfTasksToUpdate;
    }

    @Override
    public void run()
    {
        try(final Connection connection = DBConnection.get();
            final PreparedStatement stmt = connection.prepareStatement("CALL update_job_progress(?)"))
        {
            stmt.setInt(1, numOfTasksToUpdate);

            if(LOG.isDebugEnabled())
            {
                LOG.debug("Calling update_job_progress({}) database procedure ...", numOfTasksToUpdate);
                final Instant start = Instant.now();
                stmt.execute();
                final Instant end = Instant.now();
                LOG.debug("Total time taken to update job progress in ms. " + Duration.between(start, end).toMillis());
            }
            else
            {
                stmt.execute();
            }
        }
        catch(final Throwable t)
        {
            LOG.error("Caught exception while updating job progress.", t);
        }
    }
}
