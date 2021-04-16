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
import com.hpe.caf.services.job.api.generated.model.DeletePolicy.ExpirationOperationEnum;
import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.api.generated.model.Policy;
import com.hpe.caf.services.job.api.generated.model.Policy.OperationEnum;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        definePolicies(expirationPolicies);

        job.setExpiry(expirationPolicies);
    }

    private static ExpirationPolicy getExpiryPolicyFromJob(final NewJob job)
    {
        // creates new policies if none provided
        if (null != job.getExpiry()) {
            return job.getExpiry();
        } else {
            return new ExpirationPolicy();
        }
    }

    private static void definePolicies(
        final ExpirationPolicy expirationPolicies
    ) throws BadRequestException
    {
        final Policy defaultPolicy = defineDefaultGlobalPolicy(expirationPolicies);

        definePolicy(expirationPolicies::getActive, expirationPolicies::setActive, defaultPolicy);
        definePolicy(expirationPolicies::getCompleted, expirationPolicies::setCompleted, defaultPolicy);
        definePolicy(expirationPolicies::getCancelled, expirationPolicies::setCancelled, defaultPolicy);
        definePolicy(expirationPolicies::getFailed, expirationPolicies::setFailed, defaultPolicy);
        definePolicy(expirationPolicies::getWaiting, expirationPolicies::setWaiting, defaultPolicy);
        definePolicy(expirationPolicies::getPaused, expirationPolicies::setPaused, defaultPolicy);
        defineExpiredPolicy(expirationPolicies);
    }

    /**
     *
     * @param expirationPolicies the expiration object provided
     * @return the default policy
     * @throws BadRequestException if any invalid parameter
     */
    private static Policy defineDefaultGlobalPolicy(final ExpirationPolicy expirationPolicies) throws BadRequestException
    {
        // define default policy
        final Policy defaultPolicy;
        if (null != expirationPolicies.getDefault()) {
            defaultPolicy = expirationPolicies.getDefault();
            DateHelper.validate(defaultPolicy.getExpiryTime());
        } else {
            defaultPolicy = new Policy();
            defaultPolicy.setOperation(OperationEnum.EXPIRE);
            defaultPolicy.setExpiryTime("none");
        }
        return defaultPolicy;
    }

    private static void definePolicy(
        final Supplier<Policy> policySupplier,
        final Consumer<Policy> policyConsumer,
        final Policy defaultPolicy
    ) throws BadRequestException
    {
        if (null == policySupplier.get()) {
            final Policy policy = clonePolicy(defaultPolicy);
            policyConsumer.accept(policy);
        } else {
            DateHelper.validate(policySupplier.get().getExpiryTime());
        }
    }

    private static Policy clonePolicy(final Policy policy)
    {
        final Policy newPolicy = new Policy();
        newPolicy.setOperation(policy.getOperation());
        newPolicy.setExpiryTime(policy.getExpiryTime());
        return newPolicy;
    }

    private static void defineExpiredPolicy(final ExpirationPolicy expirationPolicies) throws BadRequestException
    {
        if (null == expirationPolicies.getExpired()) {
            expirationPolicies.setExpired(defineDefaultDeletePolicy());
        } else {
            DateHelper.validate(expirationPolicies.getExpired().getExpiryTime());
        }
    }

    private static DeletePolicy defineDefaultDeletePolicy()
    {
        final DeletePolicy deletePolicy = new DeletePolicy();
        deletePolicy.setExpirationOperation(ExpirationOperationEnum.DELETE);
        deletePolicy.setExpiryTime("none");
        return deletePolicy;
    }
}
