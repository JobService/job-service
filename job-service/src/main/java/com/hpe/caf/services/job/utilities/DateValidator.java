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

public final class DateValidator {

    public static void validate(final String dateToCheck) throws BadRequestException {
        final String dateRegex = "^(lastUpdateTime|createTime)(\\+P)[1-9]\\d*[DYHM]$|^none$";
        if(!dateToCheck.matches(dateRegex)){
            try{
                Instant.parse(dateToCheck);
            }catch (final DateTimeParseException e){
                throw new BadRequestException("invalid date "+dateToCheck);
            }
        }

    }
}
