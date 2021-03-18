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

import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.api.generated.model.Policy;
import com.hpe.caf.services.job.api.generated.model.Policy.OperationEnum;
import com.hpe.caf.services.job.exceptions.BadRequestException;

import java.util.HashMap;
import java.util.Map;

public final class PolicyBuilder {

    private PolicyBuilder() {
    }

    /**
     *
     * @param job the job to be created
     * @return a map of the policies for the job
     * @throws BadRequestException if any invalid parameter
     */
    public static Map<String, Policy> buildPolicyMap(final NewJob job) throws BadRequestException {
        final HashMap<String, Policy> finalExpirationPolicy = new HashMap<>();
        final ExpirationPolicy expirationPolicies;
        // create new policies if none provided
        if (null != job.getExpiry()){
            expirationPolicies = job.getExpiry();
        }else {
            expirationPolicies = new ExpirationPolicy();
        }
        final Policy defaultPolicy = defineDefaultPolicy(finalExpirationPolicy, expirationPolicies);

        // define active policy
        definePolicy(finalExpirationPolicy, defaultPolicy, expirationPolicies.getActive(), "Active");

        // define completed policy
        definePolicy(finalExpirationPolicy, defaultPolicy, expirationPolicies.getCompleted(), "Completed");

        // define failed policy
        definePolicy(finalExpirationPolicy, defaultPolicy, expirationPolicies.getFailed(), "Failed");

        // define Paused policy
        definePolicy(finalExpirationPolicy, defaultPolicy, expirationPolicies.getPaused(), "Paused");

        // define Waiting policy
        definePolicy(finalExpirationPolicy, defaultPolicy, expirationPolicies.getWaiting(), "Waiting");

        // define Cancelled policy
        definePolicy(finalExpirationPolicy, defaultPolicy, expirationPolicies.getCancelled(), "Cancelled");

        return finalExpirationPolicy;
    }

    /**
     *
     * @param finalExpirationPolicy the expiration object to build
     * @param expirationPolicies the expiration object provided
     * @return the default policy
     * @throws BadRequestException if any invalid parameter
     */
    private static Policy defineDefaultPolicy(final HashMap<String, Policy> finalExpirationPolicy,
            final ExpirationPolicy expirationPolicies) throws BadRequestException {
        // define default policy
        final Policy defaultPolicy;
        if(null != expirationPolicies.getDefault()){
            final String dateDefault = expirationPolicies.getDefault().getExpiryTime();
            DateValidator.validate(dateDefault);
            defaultPolicy = expirationPolicies.getDefault();
        }else {
            defaultPolicy = new Policy();
            defaultPolicy.setOperation(OperationEnum.EXPIRE);
            defaultPolicy.setExpiryTime(null);
        }
        finalExpirationPolicy.put("Default", defaultPolicy);
        return defaultPolicy;
    }

    /**
     *
     * @param finalExpirationPolicy the expiration object to build
     * @param defaultPolicy the default policy
     * @param providedPolicy the provided policy
     * @param jobStatus the job status for the policy provided
     * @throws BadRequestException if any invalid parameter
     */
    private static void definePolicy(final HashMap<String, Policy> finalExpirationPolicy, final Policy defaultPolicy,
            final Policy providedPolicy,
            final String jobStatus) throws BadRequestException {
        // transfer provided policy if not null
        // replace with default otherwise
        final Policy policy;
        if (null != providedPolicy) {
            final String dateActive = providedPolicy.getExpiryTime();
            DateValidator.validate(dateActive);
            policy = providedPolicy;
        } else {
            policy = defaultPolicy;
        }
        finalExpirationPolicy.put(jobStatus, policy);
    }
}
