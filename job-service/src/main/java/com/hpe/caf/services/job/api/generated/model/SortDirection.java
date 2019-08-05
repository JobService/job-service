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
 * Directions a list can be sorted in.
 */
public enum SortDirection {
    ASCENDING(true),
    DESCENDING(false);

    private final boolean dbValue;

    SortDirection(final boolean dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * @return Value as accepted by database stored procedures (true for ascending, false for
     *         descending)
     */
    public boolean getDbValue() {
        return dbValue;
    }

}
