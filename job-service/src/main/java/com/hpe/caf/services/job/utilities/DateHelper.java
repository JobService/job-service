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
package com.hpe.caf.services.job.utilities;

import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public final class DateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(DateHelper.class);

    private DateHelper() {
    }

    /**
     * P1Y  1 year
     * P1M  1 month
     * P1D  1 day
     * 1H   1 hour
     * 1M   1 minute
     * 1S   1 second
     * https://www.postgresql.org/docs/9.3/datatype-datetime.html
     * @param dateToCheck the date to validate
     * @throws BadRequestException if any invalid parameter
     */
    public static void validate(final String dateToCheck) throws BadRequestException {
        // ISO 8601 / RFC 3339 https://en.wikipedia.org/wiki/ISO_8601#Durations
        final String dateRegex = "^(lastUpdateTime|createTime)\\+((P)\\d+[DYM])$|" +
                "^(lastUpdateTime|createTime)\\+(PT)?(\\d+[HMS])$|" +
                "^none$";
        if(!dateToCheck.matches(dateRegex)){
            try{
                // validate date format
                final Instant instantPassed = Instant.parse(dateToCheck);
                // verify that date is in the future
                if (instantPassed.isBefore(Instant.now()))throw new BadRequestException("Date should be in the future ,"+dateToCheck);
            }catch (final DateTimeParseException e){
                final String errorMessage = "Invalid date "+dateToCheck;
                LOG.error(errorMessage);
                throw new BadRequestException(errorMessage);
            }
        }
    }
}
