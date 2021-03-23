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

import java.time.Instant;
import java.time.format.DateTimeParseException;

public final class DateHelper {

    private DateHelper() {
    }

    /**
     *
     * @param dateToCheck the date to validate
     * @throws BadRequestException if any invalid parameter
     */
    public static String validateAndConvert(final String dateToCheck) throws BadRequestException {
        final String dateRegex = "^(lastUpdateTime|createTime)(\\+P)[1-9]\\d*[DYHM]$|^none$";
        if(!dateToCheck.matches(dateRegex)){
            try{
                // validate date format
                final Instant instantPassed = Instant.parse(dateToCheck);
                // verify that date is in the future
                if (instantPassed.isBefore(Instant.now()))throw new BadRequestException("Date should be in the future ,"+dateToCheck);
                return dateToCheck;
            }catch (final DateTimeParseException e){
                throw new BadRequestException("Invalid date "+dateToCheck);
            }
        }else {
            return convertDate(dateToCheck);
        }
    }

    /**
     * Converts the date reference provided into "referenceDate + duration" -> unit is minute
     * @param dateToConvert date to be converted
     * @return the converted date
     */
    private static String convertDate(final String dateToConvert) {
        if (dateToConvert.equalsIgnoreCase("none"))return "none";
        final String[] firstSplit = dateToConvert.split("P");
        final String referenceDate = firstSplit[0];
        char symbol = Character.toUpperCase(firstSplit[1].charAt(firstSplit[1].length()-1));
        long duration = Long.parseLong(firstSplit[1].substring(0, firstSplit[1].length()-1));
        long finalDuration=0;
        switch (symbol){
            case 'M':
                // 1 minute
                finalDuration = duration;
                break;
            case 'H':
                // 1 hour = 60 mn
                finalDuration = duration * 60;
                break;
            case 'D':
                // 1 day = 1 440 mn
                finalDuration = duration * 1440;
                break;
            default:
                // 1 year = 525 600 mn
                finalDuration = duration * 525600;
        }

        return referenceDate+ finalDuration;
    }
}
