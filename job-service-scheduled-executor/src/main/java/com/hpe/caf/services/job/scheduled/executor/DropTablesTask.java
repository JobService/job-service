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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DropTablesTask implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(DropTablesTask.class);
    
    @Override
    public void run()
    {
        {
            try
            {
                try(
                        Connection connection = DBConnection.get();
                        PreparedStatement stmt = connection.prepareStatement("CALL  drop_deleted_task_tables()")
                )
                {
                    LOG.debug("Calling drop_deleted_task_tables() database procedure ...");
                    Instant start = Instant.now();
                    stmt.execute();
                    Instant end = Instant.now();
                    LOG.debug("Total time taken to drop tables in ms. "+ Duration.between(start, end).toMillis());
                    
                }
                catch(final SQLException e)
                {
                    final String errorMessage = MessageFormat
                            .format("Failed in call to drop_deleted_task_tables() database procedure.{0}",
                                    e.getMessage());
                    LOG.error(errorMessage);
                    throw new ScheduledExecutorException(errorMessage);
                }
            }
            catch(final Throwable t)
            {
                LOG.error("Caught exception while dropping soft deleted tables. Message:\n" + t
                        .getMessage()
                        + "StackTrace:\n" + Arrays.toString(t.getStackTrace()));
            }
        }
    }
}
