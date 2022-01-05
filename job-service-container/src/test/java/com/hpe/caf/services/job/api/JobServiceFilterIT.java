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

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.services.job.client.ApiClient;
import com.hpe.caf.services.job.client.ApiException;
import com.hpe.caf.services.job.client.api.JobsApi;
import com.hpe.caf.services.job.client.model.Job;
import com.hpe.caf.services.job.client.model.NewJob;
import com.hpe.caf.services.job.client.model.WorkerAction;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.testing.SettingNames;
import com.hpe.caf.worker.testing.SettingsProvider;
import com.hpe.caf.worker.testing.WorkerServices;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

/**
 * Integration tests for the functionality of the Job Service. (Not an end to end integration test.)
 */
public class JobServiceFilterIT
{

    private String connectionString;
    private String defaultPartitionId;
    private String correlationId;
    private JobsApi jobsApi;
    private final ApiClient client = new ApiClient();
    private Connection rabbitConn;

    @BeforeTest
    public void setup() throws Exception
    {
        connectionString = System.getenv("webserviceurl");
        client.setBasePath(connectionString);
        final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        client.setDateFormat(f);
        jobsApi = new JobsApi(client);
        final WorkerServices workerServices = WorkerServices.getDefault();
        final ConfigurationSource configurationSource = workerServices.getConfigurationSource();
        final RabbitWorkerQueueConfiguration rabbitConfiguration =
                configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class);
        rabbitConfiguration.getRabbitConfiguration()
                .setRabbitHost(SettingsProvider.defaultProvider.getSetting(SettingNames.dockerHostAddress));
        rabbitConfiguration.getRabbitConfiguration().setRabbitPort(
                        Integer.parseInt(SettingsProvider.defaultProvider.getSetting(SettingNames.rabbitmqNodePort)));
        rabbitConn = RabbitUtil.createRabbitConnection(rabbitConfiguration.getRabbitConfiguration());
    }

    @BeforeMethod
    public void setupMethod()
    {
        defaultPartitionId = UUID.randomUUID().toString();
        correlationId = "1";
    }

    @AfterTest
    public void tearDown() throws IOException
    {
        if(rabbitConn != null) {
            rabbitConn.close();
        }
    }

    @Test
    public void testLabelFiltering() throws Exception
    {
        final String jobId = UUID.randomUUID().toString();
        final String jobId1 = UUID.randomUUID().toString();
        final NewJob job = makeJob(jobId, "testFilterJobsByLabel");
        final NewJob job1 = makeJob(jobId, "testFilterJobsByLabel1");
        job.getLabels().put("label1", "value");
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId, job, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, job1, correlationId);
        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "labels.label1==value");
        assertTrue(jobs.size() == 1);
        cleanUpJobs(jobId, jobId1);
    }

    @Test
    public void testCompundFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob(jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");
        newJob1.getLabels().put("label1", "value");
        newJob2.getLabels().put("label2", "value");
        newJob3.getLabels().put("label3", "value");
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "labels.label1==value or labels.label3==value");
        assertTrue(jobs.size() == 2);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testIdFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob(jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "id==" + jobId1);
        assertTrue(jobs.size() == 1);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testIdNotConditionFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob(jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "id!=" + jobId1);
        assertTrue(jobs.size() == 2);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testNameFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob(jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "name==" + newJob1.getName());
        assertTrue(jobs.size() == 1);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testNameInFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob(jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "name=in=(" + newJob1.getName() + ","
            + newJob2.getName() + ")");
        assertTrue(jobs.size() == 2);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testNameOutFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob(jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "name=out=(" + newJob1.getName() + ")");
        assertTrue(jobs.size() == 2);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testNameLikeFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob(jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "name==Job_*");
        assertTrue(jobs.size() == 3);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testNameNotLikeFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob("test_", jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "name!=test_*");
        assertTrue(jobs.size() == 2);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testStatusInFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob("test_", jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "status=in=(Waiting,Active)");
        assertTrue(jobs.size() == 3);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    @Test
    public void testStatusOutFiltering() throws Exception
    {
        final String jobId1 = UUID.randomUUID().toString();
        final String jobId2 = UUID.randomUUID().toString();
        final String jobId3 = UUID.randomUUID().toString();
        final NewJob newJob1 = makeJob("test_" + jobId1, jobId1, "testGreaterThanAndLessThan1");
        final NewJob newJob2 = makeJob(jobId2, "testGreaterThanAndLessThan2");
        final NewJob newJob3 = makeJob(jobId3, "testGreaterThanAndLessThan3");

        jobsApi.createOrUpdateJob(defaultPartitionId, jobId1, newJob1, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId2, newJob2, correlationId);
        jobsApi.createOrUpdateJob(defaultPartitionId, jobId3, newJob3, correlationId);

        final List<Job> jobs = jobsApi.getJobs(
            defaultPartitionId, correlationId, null, null, null, null, null, null, "status=out=(Failed,Completed)");
        assertTrue(jobs.size() == 3);
        cleanUpJobs(jobId1, jobId2, jobId3);
    }

    private void cleanUpJobs(final String... jobIds) throws ApiException
    {
        for (final String jobId : jobIds) {
            jobsApi.deleteJob(defaultPartitionId, jobId, correlationId);
        }
    }

    private NewJob makeJob(final String jobId, final String testId)
    {
        final String jobName = "Job_" + jobId;
        return makeJob(jobName, jobId, testId);
    }

    private NewJob makeJob(final String jobName, final String jobId, final String testId)
    {
        //create a WorkerAction task
        final WorkerAction workerActionTask = new WorkerAction();
        workerActionTask.setTaskClassifier(jobName + "_" + testId);
        workerActionTask.setTaskApiVersion(1);
        workerActionTask.setTaskData("{\"data\" : \"" + jobName + "_TaskClassifier Sample Test Task Data.\"}");
        workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        workerActionTask.setTaskPipe("TaskQueue_" + jobId);
        workerActionTask.setTargetPipe("Queue_" + jobId);

        try {
            final Channel rabbitChannel = rabbitConn.createChannel();
            rabbitChannel.queueDeclare("TaskQueue_" + jobId, true, false, false,
                    new HashMap<>());
            rabbitChannel.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobName + " Descriptive Text.");
        newJob.setExternalData(jobName + " External data.");
        newJob.setTask(workerActionTask);

        return newJob;
    }
}
