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
            final PreparedStatement stmt = connection.prepareStatement("CALL  update_job_progress(numOfTasksToUpdate)"))
        {
            if(LOG.isDebugEnabled())
            {
                LOG.debug("Calling update_job_progress(tasksToUpdate) database procedure ...");
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
