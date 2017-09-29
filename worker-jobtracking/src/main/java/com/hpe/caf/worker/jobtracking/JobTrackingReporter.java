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
package com.hpe.caf.worker.jobtracking;

/**
 * Methods to be used to report job task progress and events to a Job Database.
 */
public interface JobTrackingReporter {

    /**
     * Reports the progress of a job task to the Job Database.
     * @param jobTaskId identifies the job task whose progress is to be reported
     * @param estimatedPercentageCompleted an indication of progress on the job task
     * @throws JobReportingException
     */
    void reportJobTaskProgress(final String jobTaskId, final int estimatedPercentageCompleted) throws JobReportingException;


    /**
     * Reports the completion of a job task to the Job Database.
     * @param jobTaskId identifies the completed job task
     * @throws JobReportingException
     */
    void reportJobTaskComplete(final String jobTaskId) throws JobReportingException;


    /**
     * Reports the failure and retry of a job task to the Job Database.
     * @param jobTaskId identifies the failed job task
     * @param retryDetails an explanation of the retry of this job task
     * @throws JobReportingException
     */
    void reportJobTaskRetry(final String jobTaskId, final String retryDetails) throws JobReportingException;


    /**
     * Reports the failure and rejection of a job task to the Job Database.
     * @param jobTaskId identifies the rejected job task
     * @param rejectionDetails an explanation of the failure and rejection of the job task
     * @throws JobReportingException
     */
    void reportJobTaskRejected(final String jobTaskId, final JobTrackingWorkerFailure rejectionDetails) throws JobReportingException;


    /**
     * Verifies that the Job Database can be contacted.
     * @return true if connection can be established with the Job Database, false otherwise
     */
    boolean verifyJobDatabase();
}
