/*
 * Copyright 2016-2023 Open Text.
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
 * Directions a list can be sorted in.
 */
public enum SortDirection {
    ASCENDING("asc", true),
    DESCENDING("desc", false);

    private static final Map<String, SortDirection> apiValueLookup = new HashMap<>();

    private final String apiValue;
    private final boolean dbValue;

    static {
        for (final SortDirection direction : values()) {
            apiValueLookup.put(direction.apiValue, direction);
        }
    }

    SortDirection(final String apiValue, final boolean dbValue) {
        this.apiValue = apiValue;
        this.dbValue = dbValue;
    }

    /**
     * @param apiValue Public identifier for this sort direction
     * @return The matching sort direction, or null
     */
    public static SortDirection fromApiValue(final String apiValue) {
        return apiValueLookup.get(apiValue);
    }

    /**
     * @return Public identifier for this sort direction
     */
    public String getApiValue() {
        return apiValue;
    }

    /**
     * @return Value as accepted by database stored procedures (true for ascending, false for
     *         descending)
     */
    public boolean getDbValue() {
        return dbValue;
    }

}
