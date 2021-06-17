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

import com.hpe.caf.services.job.api.generated.model.DeletePolicy;
import com.hpe.caf.services.job.api.generated.model.ExpirablePolicy;
import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.exceptions.BadRequestException;

public final class PolicyBuilder
{
    private PolicyBuilder()
    {
    }

    /**
     * Validates the job expiry policies and populates the job with the complete list of them.
     *
     * @param job the job to be created
     * @throws BadRequestException if any invalid parameter
     */
    public static void buildPolicyMap(final NewJob job) throws BadRequestException
    {
        final ExpirationPolicy expirationPolicies = getExpiryPolicyFromJob(job);
        job.setExpiry(expirationPolicies);
    }

    private static ExpirationPolicy getExpiryPolicyFromJob(final NewJob job) throws BadRequestException {
        if (null != job.getExpiry()) {
            checkPolicyDates(job.getExpiry());
            return job.getExpiry();
        } else {
            return new ExpirationPolicy();
        }
    }

    private static void checkPolicyDates(
        final ExpirationPolicy expirationPolicies
    ) throws BadRequestException
    {
        checkDateForExpirablePolicy(expirationPolicies.getActive());
        checkDateForExpirablePolicy(expirationPolicies.getWaiting());
        checkDateForExpirablePolicy(expirationPolicies.getPaused());
        checkDateForDeletePolicy(expirationPolicies.getCompleted());
        checkDateForDeletePolicy(expirationPolicies.getCancelled());
        checkDateForDeletePolicy(expirationPolicies.getFailed());
        checkDateForDeletePolicy(expirationPolicies.getExpired());
    }

    private static void checkDateForDeletePolicy(final DeletePolicy deletePolicy) throws BadRequestException {
        if (null != deletePolicy) {
            DateHelper.validate(deletePolicy.getExpiryTime());
        }
    }

    private static void checkDateForExpirablePolicy(final ExpirablePolicy expirablePolicy) throws BadRequestException {
        if (null != expirablePolicy) {
            DateHelper.validate(expirablePolicy.getExpiryTime());
        }
    }

}
