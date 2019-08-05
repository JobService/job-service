/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.services.job.api.generated.model;

/**
 * Fields a list of jobs can be sorted by.
 */
public enum JobSortField {
    JOB_ID("job_id"),
    CREATE_DATE("create_date");

    /**
     * The field that should be used to sort by default.
     */
    public final static JobSortField DEFAULT = CREATE_DATE;

    private final String dbField;

    JobSortField(final String dbField) {
        this.dbField = dbField;
    }

    /**
     * @return Database column name corresponding to this field.
     */
    public String getDbField() {
        return dbField;
    }

}
