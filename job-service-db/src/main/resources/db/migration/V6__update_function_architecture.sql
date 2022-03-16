--
-- Copyright 2016-2021 Micro Focus or one of its affiliates.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

/*
 **************************************************************
 ************   UPDATES FUNCTIONS ARCHITECTURE    **************
 **************************************************************
 */

DO
$$
    BEGIN

        IF EXISTS (
            -- Checks if the old architecture is used
                SELECT NULL
                FROM flyway_schema_history
                WHERE script LIKE 'procedures/R__%'
            )
        THEN
            UPDATE flyway_schema_history AS fsh SET
                script = c.script
            FROM (VALUES
                      ('cancelJob', 'functions/public/R__cancelJob.sql'),
                      ('createJob', 'functions/public/R__createJob.sql'),
                      ('createJobForDependencies', 'functions/public/R__createJobForDependencies.sql'),
                      ('deleteDependentJob', 'functions/public/R__deleteDependentJob.sql'),
                      ('deleteJob', 'functions/public/R__deleteJob.sql'),
                      ('dropDeletedTaskTables', 'procedures/public/R__dropDeletedTaskTables.sql'),
                      ('getDependentJobs', 'functions/public/R__getDependentJobs.sql'),
                      ('getJob', 'functions/public/R__getJob.sql'),
                      ('getJobs', 'functions/public/R__getJobs.sql'),
                      ('getJobsCount', 'functions/public/R__getJobsCount.sql'),
                      ('internal  cleanupCompletedSubtaskReport', 'functions/internal/R__internal__cleanupCompletedSubtaskReport.sql'),
                      ('internal  createJob', 'functions/internal/R__internal__createJob.sql'),
                      ('internal  createTaskTable', 'functions/internal/R__internal__createTaskTable.sql'),
                      ('internal  doesTableExist', 'functions/internal/R__internal__doesTableExist.sql'),
                      ('internal  dropTaskTables', 'functions/internal/R__internal__dropTaskTables.sql'),
                      ('internal  getJobId', 'functions/internal/R__internal__getJobId.sql'),
                      ('internal  getOptions', 'functions/internal/R__internal__getOptions.sql'),
                      ('internal  getParentTaskId', 'functions/internal/R__internal__getParentTaskId.sql'),
                      ('internal  getPrereqJobIdOptions', 'functions/internal/R__internal__getPrereqJobIdOptions.sql'),
                      ('internal  getSubtaskCount', 'functions/internal/R__internal__getSubtaskCount.sql'),
                      ('internal  getSubtaskId', 'functions/internal/R__internal__getSubtaskId.sql'),
                      ('internal  getTaskStatus', 'functions/internal/R__internal__getTaskStatus.sql'),
                      ('internal  getTaskTableName', 'functions/internal/R__internal__getTaskTableName.sql'),
                      ('internal  hasDependentJobs', 'functions/internal/R__internal__hasDependentJobs.sql'),
                      ('internal  insertDeleteLog', 'functions/internal/R__internal__insertDeleteLog.sql'),
                      ('internal  insertParentTableToDelete', 'functions/internal/R__internal__insertParentTableToDelete.sql'),
                      ('internal  isFinalTask', 'functions/internal/R__internal__isFinalTask.sql'),
                      ('internal  isTaskCompleted', 'functions/internal/R__internal__isTaskCompleted.sql'),
                      ('internal  populateDeleteLogTable', 'procedures/internal/R__internal__populateDeleteLogTable.sql'),
                      ('internal  processDependentJobs', 'functions/internal/R__internal__processDependentJobs.sql'),
                      ('internal  processFailedDependentJobs', 'functions/internal/R__internal__processFailedDependentJobs.sql'),
                      ('internal  reportTaskStatus', 'functions/internal/R__internal__reportTaskStatus.sql'),
                      ('internal  resolveStatus', 'functions/internal/R__internal__resolveStatus.sql'),
                      ('internal  toRegClass', 'functions/internal/R__internal__toRegClass.sql'),
                      ('internal  updateJobProgress', 'functions/internal/R__internal__updateJobProgress.sql'),
                      ('internal  upsertIntoTaskTable', 'functions/internal/R__internal__upsertIntoTaskTable.sql'),
                      ('pauseJob', 'functions/public/R__pauseJob.sql'),
                      ('reportComplete', 'functions/public/R__reportComplete.sql'),
                      ('reportCompleteBulk', 'functions/public/R__reportCompleteBulk.sql'),
                      ('reportFailure', 'functions/public/R__reportFailure.sql'),
                      ('reportProgress', 'functions/public/R__reportProgress.sql'),
                      ('resumeJob', 'functions/public/R__resumeJob.sql')
             ) AS c(description, script)
            WHERE c.description = fsh.description;

        END IF;

    END
$$;
