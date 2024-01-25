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

import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.configuration.AppConfigException;
import com.hpe.caf.services.configuration.AppConfigProvider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Load job types. Job types are loaded from definition files with names ending in '.yaml' in the globally configured directory.
 *
 * @see com.hpe.caf.services.configuration.AppConfig#getJobTypeDefinitionsDir
 */
public final class JobTypesInitialization
{
    private JobTypesInitialization()
    {
    }

    public static void initialize()
    {
        try {
            final AppConfig appConfig = AppConfigProvider.getAppConfigProperties();
            final Path dir = appConfig.getJobTypeDefinitionsDir();
            final Loader loader = dir == null ?
                new NoneLoader() :
                new DirectoryLoader(new DefaultDefinitionParser(appConfig), dir);
            JobTypes.initialise(loader);
        } catch (final AppConfigException | InvalidJobTypeDefinitionException | IOException e) {
            throw new RuntimeException("Error loading job type definitions", e);
        }
    }
}
