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
import com.fasterxml.jackson.databind.node.NullNode;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.configuration.AppConfigException;
import com.hpe.caf.services.configuration.AppConfigProvider;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.jobtype.JobTypes;
import com.hpe.caf.services.job.queue.QueueServices;
import com.hpe.caf.services.job.queue.QueueServicesFactory;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;
import com.hpe.caf.worker.document.DocumentWorkerConstants;
import com.hpe.caf.worker.document.DocumentWorkerDocumentTask;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class JobsPut {

    public static final String ERR_MSG_JOB_AND_TASK_SPECIFIED = "Specifying both typeId and task properties is not allowed.";
    public static final String ERR_MSG_TASK_DATA_NOT_SPECIFIED = "The task data has not been specified.";
    public static final String ERR_MSG_TASK_CLASSIFIER_NOT_SPECIFIED = "The task classifier has not been specified.";
    public static final String ERR_MSG_TASK_API_VERSION_NOT_SPECIFIED = "The task api version has not been specified.";
    public static final String ERR_MSG_TASK_QUEUE_NOT_SPECIFIED = "The task queue name has not been specified.";
    public static final String ERR_MSG_TASK_DATA_DATATYPE_ERROR = "The taskData is null or empty, please ensure taskData is populated and of a suitable datatype";
    public static final String ERR_MSG_TARGET_QUEUE_NOT_SPECIFIED = "The target queue name has not been specified.";
    public static final String ERR_MSG_TASK_DATA_OBJECT_ENCODING_CONFLICT = "An encoding type has been found in the task along with "
        + "taskDataObject. Remove taskDataEncoding and try again";
    public static final String ERR_MSG_INVALID_LABEL_NAME = "A provided label name contains an invalid character, only " +
            "alphanumeric, '-' and '_' are supported";

    private static final Logger LOG = LoggerFactory.getLogger(JobsPut.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern LABEL_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-:]+$");
    private static final AppConfig config = getConfig();
    private static final Codec codec = getCodec();
    
    /**
     * Creates a new job with the job object provided if the specified job id does not exist. If the job id already exists it updates
     * the existing job.
     *
     * @param   jobId           the id of the job to create or update
     * @param   job             the job object which populates the job database in creation of the new job
     * @return  targetOperation the result of the operation to be performed
     * @throws  Exception       bad request exception or database exception
     */
    public static String createOrUpdateJob(final String partitionId, String jobId, NewJob job) throws Exception {
        try {
            LOG.debug("createOrUpdateJob: Starting...");
            ApiServiceUtil.validatePartitionId(partitionId);

            //  Make sure the job id has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(jobId)) {
                LOG.error("createOrUpdateJob: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_NOT_SPECIFIED);
            }

            //  Make sure the job id does not contain any invalid characters.
            if (ApiServiceUtil.containsInvalidCharacters(jobId)) {
                LOG.error("createOrUpdateJob: Error - '{}'", ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
                throw new BadRequestException(ApiServiceUtil.ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS);
            }

            // if `job` is provided, construct `task` from it
            final WorkerAction jobTask;
            if (job.getType() == null) {
                jobTask = job.getTask();
            } else {
                if (job.getTask() != null) {
                    LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_JOB_AND_TASK_SPECIFIED);
                    throw new BadRequestException(ERR_MSG_JOB_AND_TASK_SPECIFIED);
                }

                jobTask = JobTypes.getInstance()
                    .getJobType(job.getType())
                    // treating InvalidJobDefinitionException as server error
                    .buildTask(partitionId, jobId,
                        job.getParameters() == null ? NullNode.getInstance() : job.getParameters());
            }

            //  Make sure the task classifier has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(jobTask.getTaskClassifier())) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_CLASSIFIER_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TASK_CLASSIFIER_NOT_SPECIFIED);
            }

            //  Make sure the task api version has been provided.
            if (0 == jobTask.getTaskApiVersion()) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_API_VERSION_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TASK_API_VERSION_NOT_SPECIFIED);
            }

            //  Make sure the target queue name is not empty. Null targetPipe is valid
            if ("".equals(jobTask.getTargetPipe())) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TARGET_QUEUE_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TARGET_QUEUE_NOT_SPECIFIED);
            }

            //  Make sure the task queue name has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(jobTask.getTaskPipe())) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_QUEUE_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TASK_QUEUE_NOT_SPECIFIED);
            }

            // Make sure label names are valid.
            if (job.getLabels() != null &&
                    job.getLabels().keySet().stream().anyMatch(key -> !LABEL_PATTERN.matcher(key).matches())) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_INVALID_LABEL_NAME);
                throw new BadRequestException(ERR_MSG_INVALID_LABEL_NAME);
            }
            
            final Object taskData = jobTask.getTaskData();

            // Make sure that taskData is available
            if (taskData == null) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_DATA_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TASK_DATA_NOT_SPECIFIED);
            }

            if (taskData instanceof String) {
                if (((String) taskData).isEmpty()) {
                    LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_DATA_NOT_SPECIFIED);
                    throw new BadRequestException(ERR_MSG_TASK_DATA_NOT_SPECIFIED);
                }
            } else if (taskData instanceof Map<?, ?>) {
                if (jobTask.getTaskDataEncoding() != null) {// If taskData is an object, ensure that taskDataEncoding has been left out.
                    LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_DATA_OBJECT_ENCODING_CONFLICT);
                    throw new BadRequestException(ERR_MSG_TASK_DATA_OBJECT_ENCODING_CONFLICT);
                }
            } else {
                // The taskData is not of type String or Object
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_DATA_DATATYPE_ERROR);
                throw new BadRequestException(ERR_MSG_TASK_DATA_DATATYPE_ERROR);
            }

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Create hash of parameters not being stored in the database.
            int jobHash = job.hashCode();

            // Remove all null or blank prerequisite job ids from the job's list of prerequisiteJobIds
            if (job.getPrerequisiteJobIds() != null && !job.getPrerequisiteJobIds().isEmpty()) {
                job.setPrerequisiteJobIds(job.getPrerequisiteJobIds().stream()
                        .filter(prereqJobId -> prereqJobId != null && !prereqJobId.trim().isEmpty())
                        .collect(Collectors.toList()));
            }

            final boolean partitionSuspended = ApiServiceUtil.isPartitionSuspended(config.getSuspendedPartitionsPattern(), partitionId);
            //  Create job in the database.
            LOG.debug("createOrUpdateJob: Creating job in the database for {} : {}...",
                    partitionSuspended ? "suspended partition" : "partition", partitionId);
            final boolean jobCreated;
            if ((job.getPrerequisiteJobIds() != null && !job.getPrerequisiteJobIds().isEmpty()) || partitionSuspended) {
                jobCreated = databaseHelper.createJobWithDependencies(partitionId, jobId, job.getName(), job.getDescription(),
                        job.getExternalData(), jobHash, jobTask.getTaskClassifier(), jobTask.getTaskApiVersion(),
                        getTaskDataBytes(jobTask), jobTask.getTaskPipe(), jobTask.getTargetPipe(),
                        job.getPrerequisiteJobIds(), job.getDelay(), job.getLabels(), partitionSuspended);

            } else {
                jobCreated = databaseHelper.createJob(partitionId, jobId, job.getName(), job.getDescription(),
                        job.getExternalData(), jobHash, jobTask.getTaskClassifier(), jobTask.getTaskApiVersion(),
                        getTaskDataBytes(jobTask), jobTask.getTaskPipe(), jobTask.getTargetPipe(),
                        job.getDelay(), job.getLabels());
            }

            if (!jobCreated) {
                return "update";
            }

            triggerScheduler();

            LOG.debug("createOrUpdateJob: Done.");

            return "create";
        } catch (Exception e) {
            LOG.error("Error - ", e);
            throw e;
        }
    }
    
    /**
     * We trigger the scheduler so it will pick up the created job from the database
     * then send a message to the appropriate queue
     */
    private static void triggerScheduler()
    {
        try (final QueueServices queueServices = QueueServicesFactory.create(config, config.getSchedulerQueue(), codec)){
            
            LOG.debug("createOrUpdateJob: Triggering scheduler to send data to the target queue");
            
            final TaskMessage taskMessage = new TaskMessage(
                    UUID.randomUUID().toString(),
                    DocumentWorkerConstants.DOCUMENT_TASK_NAME,
                    1,
                    OBJECT_MAPPER.writeValueAsBytes(new DocumentWorkerDocumentTask()),
                    TaskStatus.NEW_TASK,
                    Collections.emptyMap(),
                    config.getSchedulerQueue());
            final byte[] taskMessageBytes = serializingData(taskMessage);
            queueServices.publishMessage(taskMessageBytes);
        } catch (final Exception ex) {
           LOG.error("fail to ping the scheduler {}", ex.getMessage());
        }
    }
    
    private static byte[] getTaskDataBytes(final WorkerAction workerAction)
    {
        final Object taskDataObj = workerAction.getTaskData();
        final byte[] taskDataBytes;
        if (taskDataObj instanceof String) {
            final String taskDataStr = (String) taskDataObj;
            final WorkerAction.TaskDataEncodingEnum encoding = workerAction.getTaskDataEncoding();

            if (encoding == null || encoding == WorkerAction.TaskDataEncodingEnum.UTF8) {
                taskDataBytes = taskDataStr.getBytes(StandardCharsets.UTF_8);
            } else if (encoding == WorkerAction.TaskDataEncodingEnum.BASE64) {
                taskDataBytes = Base64.decodeBase64(taskDataStr);
            } else {
                throw new RuntimeException("Unknown taskDataEncoding");
            }
        } else if (taskDataObj instanceof Map<?, ?>) {
            taskDataBytes = serializingData(taskDataObj);
        } else {
            throw new RuntimeException("The taskData is an unexpected type");
        }

        return taskDataBytes;
    }
    
    private static byte[] serializingData(final Object taskDataObj)
    {
        final byte[] taskDataBytes;
        try {
            taskDataBytes = codec.serialise(taskDataObj);
        } catch (final CodecException e) {
            throw new RuntimeException("Failed to serialise TaskData", e);
        }
        return taskDataBytes;
    }
    
    
    private static Codec getCodec()
    {
        try {
            return ModuleLoader.getService(Codec.class);
        } catch (final ModuleLoaderException e) {
            throw new RuntimeException("Issue while trying to get the codec");
        }
    }
    
    private static AppConfig getConfig()
    {
        try {
            return AppConfigProvider.getAppConfigProperties();
        } catch (final AppConfigException e) {
            throw new RuntimeException("Issue while trying to get the configuration");
        }
    }
}
