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
package com.hpe.caf.services.job.jobtype;

import com.fasterxml.jackson.databind.JsonNode;
import com.hpe.caf.services.job.exceptions.BadRequestException;

/**
 * Configured with a specific job type to construct task data for a newly submitted job.
 *
 * @see com.hpe.caf.services.job.api.generated.model.WorkerAction#getTaskData
 */
public interface TaskDataBuilder {

    /**
     * Construct task data.
     *
     * @param partitionId Partition ID the job will be created in
     * @param jobId ID the new job will have
     * @param parameters Non-null input provided along with the job request; the format is generally
     *                   defined by the job type, and this method performs appropriate validation
     * @return Task data, as a JSON representation
     * @throws InvalidJobTypeDefinitionException When the job type definition is invalid in a way
     *                                           that couldn't be determined at load-time
     * @throws BadRequestException When the parameters are invalid
     */
    JsonNode build(final String partitionId, final String jobId, final JsonNode parameters)
        throws InvalidJobTypeDefinitionException, BadRequestException;

}
