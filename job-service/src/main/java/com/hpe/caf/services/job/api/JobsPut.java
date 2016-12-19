package com.hpe.caf.services.job.api;

import com.hpe.caf.api.Codec;
import com.hpe.caf.services.job.api.generated.model.Failure;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.configuration.AppConfig;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.queue.QueueServices;
import com.hpe.caf.services.job.queue.QueueServicesFactory;
import com.hpe.caf.util.ModuleLoader;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class JobsPut {

    private static final String ERR_MSG_TASK_DATA_NOT_SPECIFIED = "The task data has not been specified.";
    private static final String ERR_MSG_TASK_CLASSIFIER_NOT_SPECIFIED = "The task classifier has not been specified.";
    private static final String ERR_MSG_TASK_API_VERSION_NOT_SPECIFIED = "The task api version has not been specified.";
    private static final String ERR_MSG_TASK_QUEUE_NOT_SPECIFIED = "The task queue name has not been specified.";
    private static final String ERR_MSG_TARGET_QUEUE_NOT_SPECIFIED = "The target queue name has not been specified.";

    private static final Logger LOG = LoggerFactory.getLogger(JobsPut.class);

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

            //  Make sure task data has been provided.
            if (!ApiServiceUtil.isNotNullOrEmpty(job.getTask().getTaskData())) {
                LOG.error("createOrUpdateJob: Error - '{}'", ERR_MSG_TASK_DATA_NOT_SPECIFIED);
                throw new BadRequestException(ERR_MSG_TASK_DATA_NOT_SPECIFIED);
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

}