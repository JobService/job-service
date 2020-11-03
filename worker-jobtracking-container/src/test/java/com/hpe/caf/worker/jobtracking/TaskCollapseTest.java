/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;


/**
 * Integration tests for Job Tracking Worker.
 */
public class TaskCollapseTest extends JobTrackingWorkerIT{

    private long start;
    private final long maxExecutionTime=5000;
    private static final Logger LOG = LoggerFactory.getLogger(TaskCollapseTest.class);

    /** Basic input with collapsable and non-collapsable data*/
    @Test
    public void testTaskCollapse1() throws SQLException {
        startExecutionTimer();
        final String[] input ={"job.7*","job.1.5","job.4.5*","job.2.3*","job.1.9","job.1.8","job.1.7","job.1.6","job.1.4","job.1.3","job.1.10*","job.1.2","job.1.1","job.2.2.2*","job.2.2.1","job.2.1"};
        final String[] output={"job.7*","job.2","job.1","job.4.5*"};
        assertTrue(jobDatabase.taskCollapseTest(input, output));
        assertTrue(checkIfMaxExecutionTimeRespected(maxExecutionTime));
    }

    /** Checking that we avoid duplicates (several similar tasks passed several times as input)*/
    @Test
    public void testTaskCollapse2() throws SQLException {
        startExecutionTimer();
        final String[] input ={};
        final String[] output={"job.88", "job.89.8"};
        assertTrue(jobDatabase.taskCollapseTest(input, output));
        assertTrue(checkIfMaxExecutionTimeRespected(maxExecutionTime));
    }

    /** Dealing with big numbers */
    @Test
    public void testTaskCollapse3() throws SQLException {
        final String[] input =buildArrayFromFile("src/test/resources/testTaskCollapse/testTaskCollapse2.txt");
        final String[] output={"job.8.999999998", "job.8.999999999*"};
        startExecutionTimer();
        assertTrue(jobDatabase.taskCollapseTest(input, output));
        assertTrue(checkIfMaxExecutionTimeRespected(maxExecutionTime));
    }

    /** Checking that we avoid duplicates (several similar tasks passed several times as input)*/
    @Test
    public void testTaskCollapse4() throws SQLException {
        final String[] input =buildArrayFromFile("src/test/resources/testTaskCollapse/testTaskCollapse4.txt");
        final String[] output={"job.88", "job.89.8"};
        startExecutionTimer();
        assertTrue(jobDatabase.taskCollapseTest(input, output));
        assertTrue(checkIfMaxExecutionTimeRespected(maxExecutionTime+maxExecutionTime)); // 10seconds
    }

    /** Checking that we can handle large lists (9580 items) */
    @Test
    public void testTaskCollapse5() throws SQLException {
        final String[] input =buildArrayFromFile("src/test/resources/testTaskCollapse/testTaskCollapse5.txt");
        final String[] output={"job"};
        startExecutionTimer();
        assertTrue(jobDatabase.taskCollapseTest(input, output));
        assertTrue(checkIfMaxExecutionTimeRespected(maxExecutionTime+maxExecutionTime));// 10 seconds
    }

    private void startExecutionTimer(){
        start= System.currentTimeMillis();
    }

    private boolean checkIfMaxExecutionTimeRespected(long maxExecutionTime){
        long executionTime = start+System.currentTimeMillis();
        LOG.info("execution time: "+executionTime);
        return executionTime < maxExecutionTime;
    }

    private String[] buildArrayFromFile(String fileName){
        LOG.info("building array from file "+fileName);
        String[] taskArray = new String[0];
        File f = new File(fileName);
        try {
            Scanner in = new Scanner(f);
            while (in.hasNext()) {
                taskArray= in.nextLine().split(",");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return taskArray;
    }
}
