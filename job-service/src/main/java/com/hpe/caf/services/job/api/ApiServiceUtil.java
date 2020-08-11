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
package com.hpe.caf.services.job.api;

import java.util.regex.Pattern;

import com.hpe.caf.services.job.exceptions.BadRequestException;

/**
 * Utility class for shared functionality.
 */
public final class ApiServiceUtil {

    private static final String API_SERVICE_RESERVED_CHARACTERS_REGEX = "[.,:;*?!|()]";

    public static final String ERR_MSG_JOB_ID_NOT_SPECIFIED = "The job identifier has not been specified.";
    public static final String ERR_MSG_JOB_ID_CONTAINS_INVALID_CHARS = "The job identifier contains one or more invalid characters.";

    /**
     * Returns TRUE if the specified string is empty or null, otherwise FALSE.
     *
     * @param   str string to validate
     * @return  boolean flag
     */
    public static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Returns TRUE if the specified string contains one or more pre-defined reserved characters, otherwise FALSE.
     *
     * @param   toExamine string to validate
     * @return  boolean flag
     */
    public static boolean containsInvalidCharacters(String toExamine) {
        String[] arr = toExamine.split(API_SERVICE_RESERVED_CHARACTERS_REGEX, 2);
        return arr.length > 1;
    }

    /**
     * Check that a partition ID provided to the API is valid.
     *
     * @param partitionId
     * @throws BadRequestException When the partition ID is invalid
     */
    public static void validatePartitionId(final String partitionId) throws BadRequestException {
        if (!isNotNullOrEmpty(partitionId)) {
            throw new BadRequestException("The partition ID has not been specified.");
        } else if (partitionId.length() > 40) {
            throw new BadRequestException("The partition ID is longer than 40 characters.");
        } else if (containsInvalidCharacters(partitionId)) {
            throw new BadRequestException("The partition ID contains one or more invalid characters.");
        }
    }

    public static boolean isPartitionSuspended(final Pattern suspendedPartitionsPattern, final String partitionId)
    {
        if(suspendedPartitionsPattern == null)
        {
            return false;
        }
        return suspendedPartitionsPattern.matcher(partitionId).matches();
    }

}
