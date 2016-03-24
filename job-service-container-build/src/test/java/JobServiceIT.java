import com.hpe.caf.services.job.client.ApiClient;
import com.hpe.caf.services.job.client.ApiException;
import com.hpe.caf.services.job.client.api.JobsApi;
import com.hpe.caf.services.job.client.model.Job;
import com.hpe.caf.services.job.client.model.NewJob;
import com.hpe.caf.services.job.client.model.WorkerAction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

/**
 * Created by CS on 14/03/2016.
 */
public class JobServiceIT {

    private String connectionString;
    private String projectId;
    ApiClient client = new ApiClient();
    JobsApi jobsApi;

    @Before
    public void setup() {
        projectId = UUID.randomUUID().toString();
        connectionString = System.getenv("webserviceurl");

        //set up client to connect to the web service running on docker, and call web methods from correct address.
//        client.setApiKey(projectId);
        client.setBasePath(connectionString);

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        client.setDateFormat(f);
        jobsApi = new JobsApi(client);
    }

    @Test
    public void testCreateJob() throws ApiException {
        String jobName = "Job_testCreateJob";
        String jobDesc = "Job_testCreateJob Descriptive Text.";
        String jobId = "20";
        String jobCorrelationId = "1";
        String jobExternalData = "Job_testCreateJob External data.";

        WorkerAction workerActionTask = new WorkerAction();
        workerActionTask.setTaskClassifier("Job_testCreateJob_TaskClassifier");
        workerActionTask.setTaskApiVersion(1);
        workerActionTask.setTaskData("Job_testCreateJob_TaskClassifier Sample Test Task Data.");
        workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        workerActionTask.setTargetPipe("Job_testCreateJob_TargetPipe");

        NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobDesc);
        newJob.setExternalData(jobExternalData);
        newJob.setTask(workerActionTask);

        jobsApi.createOrUpdateJob(jobId, newJob, jobCorrelationId);

        //retrieve job using web method
        Job retrievedJob = jobsApi.getJob(jobId, jobCorrelationId);

        Assert.assertEquals(retrievedJob.getId(), jobId);
        Assert.assertEquals(retrievedJob.getName(), newJob.getName());
        Assert.assertEquals(retrievedJob.getDescription(), newJob.getDescription());
        Assert.assertEquals(retrievedJob.getExternalData(), newJob.getExternalData());
    }

    @Test
    public void testDeleteJob() throws ApiException {
        //create a job
        String jobName = "Job_testDeleteJob";
        String jobDesc = "Job_testDeleteJob Descriptive Text.";
        String jobId = "30";
        String jobCorrelationId = "1";
        String jobExternalData = "Job_testDeleteJob External data.";

        WorkerAction workerActionTask = new WorkerAction();
        workerActionTask.setTaskClassifier("Job_testDeleteJob_TaskClassifier");
        workerActionTask.setTaskApiVersion(1);
        workerActionTask.setTaskData("Job_testDeleteJob Sample Test Task Data.");
        workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        workerActionTask.setTargetPipe("Job_testDeleteJob_TargetPipe");

        NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobDesc);
        newJob.setExternalData(jobExternalData);
        newJob.setTask(workerActionTask);

        jobsApi.createOrUpdateJob(jobId, newJob, jobCorrelationId);

        //make sure the job is there
        Job retrievedJob = jobsApi.getJob(jobId, jobCorrelationId);
        Assert.assertEquals(retrievedJob.getId(), jobId);

        //delete the job
        jobsApi.deleteJob(jobId, jobCorrelationId);

        //make sure the job does not exist
        try {
            jobsApi.getJob(jobId, jobCorrelationId).getDescription();
        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(), "ERROR: job_id {" +jobId +"} not found");
        }

    }

    @Test
    public void testRetrieveMultipleJobs() throws ApiException {
        //to test functionality of returning Jobs based on cafCorrelationId
        for(int i=0; i<10; i++){
            String jobName = "Job"+i +"_testRetrieveMultipleJobs";
            String jobDesc = "Job"+i +"_testRetrieveMultipleJobs Descriptive Text.";
            String jobId = ""+i;
            String jobCorrelationId = "100";
            String jobExternalData = "Job"+i +"_testRetrieveMultipleJobs External data.";

            WorkerAction workerActionTask = new WorkerAction();
            workerActionTask.setTaskClassifier("Job"+i +"_testRetrieveMultipleJobs_TaskClassifier");
            workerActionTask.setTaskApiVersion(1);
            workerActionTask.setTaskData("Sample Test Task Data.");
            workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
            workerActionTask.setTargetPipe("TestRetrieveMultipleJobs_TargetPipe");

            NewJob newJob = new NewJob();
            newJob.setName(jobName);
            newJob.setDescription(jobDesc);
            newJob.setExternalData(jobExternalData);
            newJob.setTask(workerActionTask);

            jobsApi.createOrUpdateJob(jobId, newJob, jobCorrelationId);
        }

        //retrieve the jobs
        List<Job> retrievedJobs = jobsApi.getJobs("100");

        //test to make sure at least the 10 jobs created are returned. Unable to filter by cafCorrelationID
        Assert.assertTrue(retrievedJobs.size()>=10);

        for(int i=0; i<10; i++) {
            //only assert if the job is one of the jobs created above (the getJobs returns ALL jobs)
            if(retrievedJobs.get(i).getId().equals(""+i)) {
                Assert.assertEquals(retrievedJobs.get(i).getId(), "" + i);
                Assert.assertEquals(retrievedJobs.get(i).getName(), "Job" + i + "_testRetrieveMultipleJobs");
                Assert.assertEquals(retrievedJobs.get(i).getDescription(), "Job" + i + "_testRetrieveMultipleJobs Descriptive Text.");
                Assert.assertEquals(retrievedJobs.get(i).getExternalData(), "Job" + i + "_testRetrieveMultipleJobs External data.");
            }
        }
    }

//    @Test
//    public void testJobIsActive() throws ApiException {
//
//        String jobName = "Job_testJobIsActive";
//        String jobDesc = "Job_testJobIsActive Descriptive Text.";
//        String jobId = "40";
//        String jobCorrelationId = "1";
//        String jobExternalData = "Job_testJobIsActive External data.";
//
//        WorkerAction workerActionTask = new WorkerAction();
//        workerActionTask.setTaskClassifier("Job_testJobIsActive");
//        workerActionTask.setTaskApiVersion(1);
//        workerActionTask.setTaskData("Job_testJobIsActive_TaskClassifier Sample Test Task Data.");
//        workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
//        workerActionTask.setTargetPipe("Job_testJobIsActive_TargetPipe");
//
//        NewJob newJob = new NewJob();
//        newJob.setName(jobName);
//        newJob.setDescription(jobDesc);
//        newJob.setExternalData(jobExternalData);
//        newJob.setTask(workerActionTask);
//
//        jobsApi.createOrUpdateJob(jobId, newJob, jobCorrelationId);
//
//        //send a task to the job here.
//
//        Boolean active = jobsApi.getJobActive(jobId, jobCorrelationId);
//
//        Assert.assertTrue(active);
//    }

    @Test
    public void testCancelJob() throws ApiException {

        String jobName = "Job_testCancelJob";
        String jobDesc = "Job_testCancelJob Descriptive Text.";
        String jobId = "40";
        String jobCorrelationId = "1";
        String jobExternalData = "Job_testCancelJob External data.";

        WorkerAction workerActionTask = new WorkerAction();
        workerActionTask.setTaskClassifier("Job_testCancelJob");
        workerActionTask.setTaskApiVersion(1);
        workerActionTask.setTaskData("Job_testCancelJob_TaskClassifier Sample Test Task Data.");
        workerActionTask.setTaskDataEncoding(WorkerAction.TaskDataEncodingEnum.UTF8);
        workerActionTask.setTargetPipe("Job_testCancelJob_TargetPipe");

        NewJob newJob = new NewJob();
        newJob.setName(jobName);
        newJob.setDescription(jobDesc);
        newJob.setExternalData(jobExternalData);
        newJob.setTask(workerActionTask);

        jobsApi.createOrUpdateJob(jobId, newJob, jobCorrelationId);

        jobsApi.cancelJob(jobId, jobCorrelationId);

        Job retrievedJob = jobsApi.getJob(jobId, jobCorrelationId);

        Assert.assertEquals(retrievedJob.getStatus(), Job.StatusEnum.CANCELLED);
    }
}
