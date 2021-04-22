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
package com.hpe.caf.services.job.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.configuration.AppConfigProvider;
import com.hpe.caf.services.job.api.generated.model.Job;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.queue.QueueServices;
import com.hpe.caf.services.job.queue.QueueServicesFactory;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsResume
{
    private static final Logger LOG = LoggerFactory.getLogger(JobsResume.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Resume the job specified by the jobId.
     *
     * @param partitionId the partition containing the id of the job to resume
     * @param jobId the id of the job to resume
     * @throws Exception bad request, database or queue exceptions thrown
     */
    public static void resumeJob(final String partitionId, final String jobId) throws Exception
    {

        try {
            LOG.debug("Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

            //  Make sure the job id has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(jobId)) {
                LOG.error("Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
            }

            //  Make sure the job id does not contain any invalid characters.
            if (ApiServiceUtil.containsInvalidCharacters(jobId)) {
                LOG.error("Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
            }

            //  Get app config settings.
            LOG.debug("Reading database connection properties...");
            final AppConfig config = AppConfigProvider.getAppConfigProperties();

            //  Get database helper instance.
            final DatabaseHelper databaseHelper = new DatabaseHelper(config);

            // Get the status of the job.
            final Job.StatusEnum jobStatus = databaseHelper.getJobStatus(partitionId, jobId);

            // Validate the job can be resumed.
            if (jobStatus != Job.StatusEnum.PAUSED && jobStatus != Job.StatusEnum.ACTIVE) {
                final String errorMessage = String.format("job_id %s cannot be resumed as it has a status of %s. Only jobs with a "
                    + "status of Paused can be resumed.", jobId, jobStatus);
                throw new BadRequestException(errorMessage);
            }

            // Resume the job if its paused, do nothing if already active.
            if (jobStatus == Job.StatusEnum.PAUSED) {
                final String resumeJobQueue = config.getResumeJobQueue();
                try {
                    // Send a message to the resume job queue.
                    LOG.debug("Sending a message to the {} queue...", resumeJobQueue);
                    sendResumeJobMessage(partitionId, jobId, resumeJobQueue, config);
                } catch (final Exception messagingException) {
                    final String errorMessage = String.format("Failed to send a message to the %s queue.", resumeJobQueue);
                    throw new Exception(errorMessage, messagingException);
                }
                //  Update the status of the job in the database.
                LOG.debug("Updating the status of the job in the database...");
                databaseHelper.resumeJob(partitionId, jobId);
            }

            LOG.debug("Done.");
        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString(), e);
            throw e;
        }
    }

    private static void sendResumeJobMessage(
        final String partitionId,
        final String jobId,
        final String resumeJobQueue,
        final AppConfig config)
        throws IOException, TimeoutException, InterruptedException, ModuleLoaderException, Exception
    {
        final Codec codec = ModuleLoader.getService(Codec.class);
        try (final QueueServices queueServices = QueueServicesFactory.create(config, resumeJobQueue, codec)) {
            final WorkerAction resumeJobWorkerAction = createResumeJobWorkerAction(partitionId, jobId, resumeJobQueue);
            queueServices.sendMessage(partitionId, jobId, resumeJobWorkerAction, config, false);
        }
    }

    private static WorkerAction createResumeJobWorkerAction(
        final String partitionId,
        final String jobId,
        final String resumeJobQueue) throws CodecException
    {
        final WorkerAction workerAction = new WorkerAction();
        workerAction.setTaskClassifier(DocumentWorkerConstants.DOCUMENT_TASK_NAME);
        workerAction.setTaskApiVersion(1);
        workerAction.setTaskData(createResumeJobTaskData(partitionId, jobId));
        workerAction.setTaskPipe(resumeJobQueue);
        return workerAction;
    }

    private static Map<String, byte[]> createResumeJobTaskData(
        final String partitionId,
        final String jobId) throws CodecException
    {
        final DocumentWorkerDocumentTask documentWorkerDocumentTask = new DocumentWorkerDocumentTask();
        final Map<String, String> customData = new HashMap<>();
        customData.put("partitionId", partitionId);
        customData.put("jobId", jobId);
        documentWorkerDocumentTask.customData = customData;
        return OBJECT_MAPPER.convertValue(documentWorkerDocumentTask, Map.class);
    }
}
