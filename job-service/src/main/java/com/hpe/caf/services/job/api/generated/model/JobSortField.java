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
package com.hpe.caf.services.job.api.generated.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Fields a list of jobs can be sorted by.
 */
public enum JobSortField {
    JOB_ID("jobId", "job_id"),
    CREATE_DATE("createTime", "create_date");

    private static final Map<String, JobSortField> apiValueLookup = new HashMap<>();

    private final String apiValue;
    private final String dbField;

    static {
        for (final JobSortField field : values()) {
            apiValueLookup.put(field.apiValue, field);
        }
    }

    JobSortField(final String apiValue, final String dbField) {
        this.apiValue = apiValue;
        this.dbField = dbField;
    }

    /**
     * @param apiValue Public identifier for this field
     * @return The matching field, or null
     */
    public static JobSortField fromApiValue(final String apiValue) {
        return apiValueLookup.get(apiValue);
    }

    /**
     * @return Public identifier for this field
     */
    public String getApiValue() {
        return apiValue;
    }

    /**
     * @return Database column name corresponding to this field.
     */
    public String getDbField() {
        return dbField;
    }

}
