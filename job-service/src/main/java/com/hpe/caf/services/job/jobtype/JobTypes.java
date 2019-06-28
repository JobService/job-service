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

import com.hpe.caf.services.job.exceptions.BadRequestException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Container of loaded job types.
 */
public final class JobTypes {
    /**
     * The global instance.
     */
    private static JobTypes INSTANCE;

    /**
     * Job type lookup by ID.
     */
    private final Map<String, JobType> jobTypes;

    private JobTypes(final Loader loader)
        throws InvalidJobTypeDefinitionException, IOException
    {
        jobTypes = new HashMap<>();
        for (final JobType defn : loader.load()) {
            final String id = defn.getId();
            if (jobTypes.containsKey(id)) {
                throw new InvalidJobTypeDefinitionException("Duplicate job type ID: " + id);
            }
            jobTypes.put(id, defn);
        }
    }

    /**
     * Load job types using the given loader, and store them in the global instance.
     *
     * @param loader
     * @throws InvalidJobTypeDefinitionException
     *         When a definition is invalid, or multiple definitions have the same ID
     * @throws IOException
     * @see #getInstance
     */
    public static void initialise(final Loader loader)
        throws InvalidJobTypeDefinitionException, IOException
    {
        INSTANCE = new JobTypes(loader);
    }

    /**
     * @return The global job types container instance
     * @throws IllegalStateException If {@link #initialise} has not yet been called
     */
    public static JobTypes getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Job types have not been loaded");
        }
        return INSTANCE;
    }

    /**
     * @param id Job type ID to look up
     * @return Job type with the given ID
     * @throws BadRequestException If there is no job type with the given ID available
     */
    public JobType getJobType(final String id) throws BadRequestException {
        final JobType jobType = jobTypes.get(id);
        if (jobType == null) {
            throw new BadRequestException("Unknown job type ID: " + id);
        }
        return jobType;
    }

}
