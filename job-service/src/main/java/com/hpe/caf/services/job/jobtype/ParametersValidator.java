/*
 * Copyright 2016-2024 Open Text.
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
package com.hpe.caf.services.job.jobtype;

import com.fasterxml.jackson.databind.JsonNode;
import com.hpe.caf.services.job.exceptions.BadRequestException;

/**
 * Validate parameters submitted with the job for use as input to the {@link TaskDataBuilder}.
 */
interface ParametersValidator {

    /**
     * @param parameters Input to validate, as a JSON representation
     * @throws BadRequestException When parameters are invalid
     */
    void validate(final JsonNode parameters) throws BadRequestException;

}
