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
package com.hpe.caf.services.job.api.performance;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.hpe.caf.services.job.api.JobServiceConnectionUtil;

public class JobServicePerformanceIT
{
    private static final Logger LOG = LoggerFactory.getLogger(JobServicePerformanceIT.class);
    
    @Test(enabled = false)
    public void testDeleteLog() throws SQLException
    {
        //prepare
        List<String> droppedTables = new ArrayList();
        try(final java.sql.Connection dbConnection = JobServiceConnectionUtil.getDbConnection())
        {
            int totalCount = Integer.parseInt(System.getProperty("task.table.deletion.count"));
            LOG.info("Creating " + totalCount + " tables ");
            Instant startTableCreation = Instant.now();
            IntStream
                    .range(1, totalCount)
                    .forEach((count) -> {
                        try(final CallableStatement createTaskTableStmt = dbConnection.prepareCall("{call internal_create_task_table(?)}");
                            final CallableStatement insertDeleteLogStmt = dbConnection.prepareCall("{call internal_insert_delete_log(?)}"))
                        {
                            String tableName = "randomTestTable_" + count;
                            createTaskTableStmt.setString(1, tableName);
                            createTaskTableStmt.executeQuery();
                            insertDeleteLogStmt.setString(1, tableName);
                            insertDeleteLogStmt.executeQuery();
                            droppedTables.add(tableName);
                        }
                        catch(SQLException throwables)
                        {
                            throwables.printStackTrace();
                        }
                    });
            Instant endTableCreation = Instant.now();
            LOG.info("Total time taken to create " + totalCount + " tables in ms. " + Duration.between(startTableCreation, endTableCreation).toMillis());
            
            // assert number of rows in delete_log to be totalCount - 1
            assertEquals(getRowsInDeleteLog(dbConnection), totalCount - 1);
            List<String> foundTables = getAllTablesByPattern(dbConnection);
            assertTrue(foundTables.containsAll(droppedTables));
            
            //act
            try(final PreparedStatement dropTables = dbConnection.prepareStatement("call drop_deleted_task_tables()"))
            {
                Instant start = Instant.now();
                dropTables.execute();
                Instant end = Instant.now();
                LOG.info("Total time taken to drop " + totalCount + " tables in ms. " + Duration.between(start, end).toMillis());
            }
            
            //assert
            foundTables = getAllTablesByPattern(dbConnection);
            assertEquals(foundTables.size(), 0);
            // assert number of rows in delete_log to be 0.
            assertEquals(getRowsInDeleteLog(dbConnection), 0);
        }
    }
    
    private List<String> getAllTablesByPattern(java.sql.Connection dbConnection) throws SQLException
    {
        List<String> foundTables = new ArrayList();
        DatabaseMetaData dbm = dbConnection.getMetaData();
        try(ResultSet rs = dbm.getTables(null, "public", "randomTestTable_%", null))
        {
            while(rs.next())
            {
                foundTables.add(rs.getString("TABLE_NAME"));
            }
        }
        return foundTables;
    }
    
    private int getRowsInDeleteLog(java.sql.Connection dbConnection) throws SQLException
    {
        try(final PreparedStatement deleteLogCount = dbConnection.prepareStatement("select count(*) from delete_log");
            final ResultSet resultSet = deleteLogCount.executeQuery())
        {
            if(resultSet.next())
            {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }
}
