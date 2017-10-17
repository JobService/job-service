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
package com.hpe.caf.services.job.api;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.services.job.api.generated.model.Failure;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.api.generated.model.WorkerAction;
import com.hpe.caf.services.job.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.queue.QueueServices;
import com.hpe.caf.services.job.queue.QueueServicesFactory;
import com.hpe.caf.util.ModuleLoader;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class JobsPut {

    public static final String ERR_MSG_TASK_DATA_NOT_SPECIFIED = "The task data has not been specified.";
    public static final String ERR_MSG_TASK_CLASSIFIER_NOT_SPECIFIED = "The task classifier has not been specified.";
    public static final String ERR_MSG_TASK_API_VERSION_NOT_SPECIFIED = "The task api version has not been specified.";
    public static final String ERR_MSG_TASK_QUEUE_NOT_SPECIFIED = "The task queue name has not been specified.";
    public static final String ERR_MSG_TASK_DATA_DATATYPE_ERROR = "The taskData is null or empty, please ensure taskData is populated and of a suitable datatype";
    public static final String ERR_MSG_TARGET_QUEUE_NOT_SPECIFIED = "The target queue name has not been specified.";
    public static final String ERR_MSG_TASK_DATA_OBJECT_ENCODING_CONFLICT = "An encoding type has been found in the task along with "
        + "taskDataObject. Remove taskDataEncoding and try again";

    private static final Logger LOG = LoggerFactory.getLogger(JobsPut.class);

    /**
     * Creates a new job with the job object provided if the specified job id does not exist. If the job id already exists it updates
     * the existing job.
     *
     * @param   jobId           the id of the job to create or update
     * @param   job             the job object which populates the job database in creation of the new job
     * @return  targetOperation the result of the operation to be performed
     * @throws  Exception       bad request exception or database exception
     */
    public static String createOrUpdateJob(String jobId, NewJob job) throws Exception {
        String targetOperation;

        try {
            LOG.info("createOrUpdateJob: Starting...");

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

            //  Make sure the task classifier has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(job.getTask().getTaskClassifier())) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_CLASSIFIER_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TASK_CLASSIFIER_NOT_SPECIFIED);
            }

            //  Make sure the task api version has been provided.
            if (0 == job.getTask().getTaskApiVersion()) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_API_VERSION_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TASK_API_VERSION_NOT_SPECIFIED);
            }

            //  Make sure the target queue name has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(job.getTask().getTargetPipe())) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TARGET_QUEUE_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TARGET_QUEUE_NOT_SPECIFIED);
            }

            //  Make sure the task queue name has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(job.getTask().getTaskPipe())) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_QUEUE_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TASK_QUEUE_NOT_SPECIFIED);
            }
            
            final Object taskData = job.getTask().getTaskData();

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
                if (job.getTask().getTaskDataEncoding() != null) {// If taskData is an object, ensure that taskDataEncoding has been left out.
                    LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_DATA_OBJECT_ENCODING_CONFLICT);
                    throw new BadRequestException(ERR_MSG_TASK_DATA_OBJECT_ENCODING_CONFLICT);
                }
            } else {
                // The taskData is not of type String or Object
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_DATA_DATATYPE_ERROR);
                throw new BadRequestException(ERR_MSG_TASK_DATA_DATATYPE_ERROR);
            }

            //  Load serialization class.
            Codec codec = ModuleLoader.getService(Codec.class);

            //  Get app config settings.
            LOG.debug("createOrUpdateJob: Reading database and RabbitMQ connection properties...");
            AppConfig config = ApiServiceUtil.getAppConfigProperties();

            //  Get database helper instance.
            DatabaseHelper databaseHelper = new DatabaseHelper(config);

            //  Create hash of parameters not being stored in the database.
            int jobHash = job.getTask().hashCode();

            //  Check first if there is anything to do.
            boolean rowExists = databaseHelper.doesJobAlreadyExist(jobId, jobHash);
            if (!rowExists){
                targetOperation = "create";


                //  Create job in the database.
                LOG.info("createOrUpdateJob: Creating job in the database...");
                databaseHelper.createJob(jobId, job.getName(), job.getDescription(), job.getExternalData(), jobHash);

                // TODO: if the job can be started then proceed as normal, else if a prereq job has not completed store the task data in the JobTaskData table. Store Dependency details in the JobDependency table. return 202 code if job cannot be started because one or more of the job.prereqJobs have not been completed yet
                // All prerequisite jobs must be complete to start the job
                if (job.getPrerequisiteJobIds() != null) {
                    final List<String> prerequisiteJobIds = job.getPrerequisiteJobIds();
                    for (final String prerequisiteJobId : prerequisiteJobIds) {
                        // If the prerequisite job has not completed then store the task data in the JobTaskData table
                        // and store dependency details in the JobDependency table
                        if (!databaseHelper.isJobComplete(prerequisiteJobId)) {

                            final WorkerAction jobTask = job.getTask();
                            // Store the job task in the JobTaskData table
                            databaseHelper.createJobTaskData(jobId, jobTask.getTaskClassifier(),
                                    jobTask.getTaskApiVersion(), getTaskDataBytes(jobTask, codec), jobTask.getTaskPipe(),
                                    jobTask.getTargetPipe());

                            // Store the dependency details of the job into the JobDependency table
                            storeJobDependencies(jobId, databaseHelper, prerequisiteJobIds);

                            // Return that the job was accepted
                            return "accept";
                        }
                    }
                }

                //  Get database helper instance.
                try {
                    QueueServices queueServices = QueueServicesFactory.create(config, job.getTask().getTaskPipe(),codec);
                    LOG.info("createOrUpdateJob: Sending task data to the target queue...");
                    queueServices.sendMessage(jobId, job.getTask(), config);
                    queueServices.close();
                } catch(Exception ex) {
                    //  Failure adding job data to queue. Update the job with the failure details.
                    Failure f = new Failure();
                    f.setFailureId("ADD_TO_QUEUE_FAILURE");
                    f.setFailureTime(new Date());
                    f.failureSource("Job Service - PUT /jobs/{"+jobId+"}");
                    f.failureMessage(ex.getMessage());

                    ObjectMapper mapper = new ObjectMapper();
                    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    mapper.setDateFormat(df);
                    databaseHelper.reportFailure(jobId, mapper.writeValueAsString(f));

                    //  Throw error message to user.
                    throw new Exception("Failed to add task data to the queue.");
                }
            }
            else {
                targetOperation = "update";
                LOG.debug("createOrUpdateJob: Matching job already exists so nothing to do...");
            }

            LOG.info("createOrUpdateJob: Done.");

            return targetOperation;
        } catch (Exception e) {
            LOG.error("Error - '{}'", e.toString());
            throw e;
        }
    }

    private static void storeJobDependencies(final String jobId, final DatabaseHelper databaseHelper,
                                             final List<String> prerequisiteJobIds) throws Exception
    {
        // Store prerequisiteJobIds into the JobDependency table
        for (final String prerequisiteJobId : prerequisiteJobIds) {
            databaseHelper.createJobDependency(jobId, prerequisiteJobId);
        }
    }

    private static byte[] getTaskDataBytes(final WorkerAction workerAction, final Codec codec)
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
            try {
                taskDataBytes = codec.serialise(taskDataObj);
            } catch (CodecException e) {
                throw new RuntimeException("Failed to serialise TaskData", e);
            }
        } else {
            throw new RuntimeException("The taskData is an unexpected type");
        }

        return taskDataBytes;
    }

}