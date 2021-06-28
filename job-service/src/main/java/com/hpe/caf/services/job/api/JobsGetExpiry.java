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
package com.hpe.caf.services.job.api;

import com.hpe.caf.services.configuration.AppConfig;
import com.hpe.caf.services.configuration.AppConfigProvider;
import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobsGetExpiry {

    private static final Logger LOG = LoggerFactory.getLogger(JobsGetExpiry.class);

    /**
     * Gets the default job expiry setting.
     *
     * @return  expiration policy
     * @throws Exception    bad request or database exceptions
     */
    public static ExpirationPolicy getDefault() throws Exception {

        final ExpirationPolicy expirablePolicy;

        try {
            LOG.debug("getDefaultExpirationPolicy: Starting...");

            //  Get app config settings.
            LOG.debug("getDefaultExpirationPolicy: Reading database connection properties...");
            final AppConfig config = AppConfigProvider.getAppConfigProperties();

            //  Get database helper instance.
            final DatabaseHelper databaseHelper = new DatabaseHelper(config);


            expirablePolicy = databaseHelper.getDefaultExpirationPolicy();
        } catch (final Exception e) {
            LOG.error("Error - ", e);
            throw e;
        }


        LOG.debug("getDefaultExpirationPolicy: Done.");
        return expirablePolicy;
    }
}
