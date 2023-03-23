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
package com.hpe.caf.services.job.scheduled.executor;

import com.hpe.caf.api.Codec;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to poll the Job Service database for jobs that can now run.
 */
public class DatabasePoller
{
    private static final Logger LOG = LoggerFactory.getLogger(DatabasePoller.class);

    public static void pollDatabaseForJobsToRun() {
        try {
            //  Poll database for prerequisite jobs that are now available to be run.
            LOG.debug("Polling Job Service database for jobs to run ...");
            final List<JobTaskData> jobsToRun = getDependentJobsToRun();

            //  Determine if there are any jobs to run.
            if (jobsToRun.size() > 0) {
                //  Load serialization class.
                LOG.debug("Loading serialization class ...");
                final Codec codec = ModuleLoader.getService(Codec.class);

                //  For each job to run, submit message to the rabbitMQ queue for further processing.
                for (final JobTaskData jtd : jobsToRun) {
                    LOG.info(MessageFormat.format("Processing job id {0} ...", jtd.getJobId()));

                    final WorkerAction workerAction = new WorkerAction();
                    workerAction.setTaskClassifier(jtd.getTaskClassifier());
                    workerAction.setTaskApiVersion(jtd.getTaskApiVersion());
                    if (jtd.getTaskData() != null) {
                        workerAction.setTaskData(new String(jtd.getTaskData(), StandardCharsets.UTF_8));
                    }
                    workerAction.setTaskPipe(jtd.getTaskPipe());
                    workerAction.setTargetPipe(jtd.getTargetPipe());
                    sendMessageToQueueMessaging(codec, jtd, workerAction);
                }
            }
        } catch (final ScheduledExecutorException e) {
            LOG.error(MessageFormat.format("Exception caught polling the Job Service database for jobs to run. {0}", e.getMessage()));
        } catch (final ModuleLoaderException e) {
            LOG.error(MessageFormat.format("Exception caught when loading the serialization class. {0}", e.getMessage()));
        }
    }

    private static void sendMessageToQueueMessaging(final Codec codec, final JobTaskData jtd, final WorkerAction workerAction)
    {
        try (final QueueServices queueServices= QueueServicesFactory.create(jtd.getTaskPipe(), jtd.getPartitionId(), codec)){
            queueServices.sendMessage(jtd.getPartitionId(), jtd.getJobId(), workerAction);
            deleteDependentJob(jtd.getPartitionId(), jtd.getJobId());
        } catch(final Exception ex) {
            LOG.error(MessageFormat.format(
                    "Exception thrown during processing of job with partition ID {0}, job ID {1} and task data {2}",
                    jtd.getPartitionId(), jtd.getJobId(), workerAction), ex);
        }
    }
    
    /**
     * Deletes the supplied job from the job_task_data database table.
     */
    private static void deleteDependentJob(final String partitionId, final String jobId) throws ScheduledExecutorException
    {
        try (
                Connection connection = DBConnection.get();
                CallableStatement stmt = connection.prepareCall("{call delete_dependent_job(?,?)}")) {
            stmt.setString(1, partitionId);
            stmt.setString(2, jobId);
            LOG.info(MessageFormat.format("Calling delete_dependent_job({0},{1}) database function ...", partitionId, jobId));
            stmt.execute();
        } catch (final SQLException e) {
            final String errorMessage = MessageFormat.format("Failed in call to delete_dependent_job({0},{1}) database function.{3}",
                    partitionId, jobId,e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }
    }

    /**
     * Returns a list of dependent jobs that are now available to run.
     */
    private static List<JobTaskData> getDependentJobsToRun() throws ScheduledExecutorException
    {
        /*
        SCMOD-6525 - FALSE POSITIVE on FORTIFY SCAN for Unreleased Resource: Database.
        */
        try (
                Connection connection = DBConnection.get();
                CallableStatement stmt = connection.prepareCall("{call get_dependent_jobs()}")
        ) {
            LOG.debug("Calling get_dependent_jobs() database function ...");
            stmt.execute();

            final List<JobTaskData> jobTaskDataList = new ArrayList<>();
            final ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                final JobTaskData dependency = new JobTaskData();
                dependency.setPartitionId(stmt.getResultSet().getString(1));
                dependency.setJobId(stmt.getResultSet().getString(2));
                dependency.setTaskClassifier(stmt.getResultSet().getString(3));
                dependency.setTaskApiVersion(stmt.getResultSet().getInt(4));
                dependency.setTaskData(stmt.getResultSet().getBytes(5));
                dependency.setTaskPipe(stmt.getResultSet().getString(6));
                dependency.setTargetPipe(stmt.getResultSet().getString(7));

                jobTaskDataList.add(dependency);
            }

            return jobTaskDataList;
        } catch (final SQLException e) {
            final String errorMessage = MessageFormat.format("Failed in call to get_dependent_jobs() database function.{0}", e.getMessage());
            LOG.error(errorMessage);
            throw new ScheduledExecutorException(errorMessage);
        }
    }

}
