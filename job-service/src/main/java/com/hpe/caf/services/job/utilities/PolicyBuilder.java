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
import com.hpe.caf.services.job.api.generated.model.DeletePolicy.OperationEnum;
import com.hpe.caf.services.job.api.generated.model.ExpirablePolicy;
import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import com.hpe.caf.services.job.api.generated.model.NewJob;
import com.hpe.caf.services.job.exceptions.BadRequestException;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PolicyBuilder
{
    private static final String SYSTEM_FLAG = "+SYSTEM";
    private static final ExpirablePolicy.OperationEnum SYSTEM_DEFAULT_OPERATION= ExpirablePolicy.OperationEnum.EXPIRE;
    private static final String SYSTEM_DEFAULT_EXPIRY_TIME = "createTime+P90M"+SYSTEM_FLAG;
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
        final ExpirablePolicy defaultExpirablePolicy = defineDefaultExpirablePolicy();
        final DeletePolicy defaultDeletePolicy = defineDefaultDeletePolicy();

        defineExpirablePolicy(expirationPolicies::getActive, expirationPolicies::setActive, defaultExpirablePolicy);
        defineExpirablePolicy(expirationPolicies::getWaiting, expirationPolicies::setWaiting, defaultExpirablePolicy);
        defineExpirablePolicy(expirationPolicies::getPaused, expirationPolicies::setPaused, defaultExpirablePolicy);
        defineDeletePolicy(expirationPolicies::getCompleted, expirationPolicies::setCompleted, defaultDeletePolicy);
        defineDeletePolicy(expirationPolicies::getCancelled, expirationPolicies::setCancelled, defaultDeletePolicy);
        defineDeletePolicy(expirationPolicies::getFailed, expirationPolicies::setFailed, defaultDeletePolicy);
        defineDeletePolicy(expirationPolicies::getExpired, expirationPolicies::setExpired, defaultDeletePolicy);
    }

    /**
     *
     * @return the default policy
     * @throws BadRequestException if any invalid parameter
     */
    private static ExpirablePolicy defineDefaultExpirablePolicy() {
        // defines default expirablePolicy
        final ExpirablePolicy defaultExpirablePolicy;
        defaultExpirablePolicy = new ExpirablePolicy();
        defaultExpirablePolicy.setOperation(SYSTEM_DEFAULT_OPERATION);
        defaultExpirablePolicy.setExpiryTime(SYSTEM_DEFAULT_EXPIRY_TIME);
        return defaultExpirablePolicy;
    }

    private static void defineExpirablePolicy(
        final Supplier<ExpirablePolicy> policySupplier,
        final Consumer<ExpirablePolicy> policyConsumer,
        final ExpirablePolicy defaultExpirablePolicy
    ) throws BadRequestException
    {
        if (null == policySupplier.get()) {
            final ExpirablePolicy expirablePolicy = cloneExpirablePolicy(defaultExpirablePolicy);
            policyConsumer.accept(expirablePolicy);
        } else {
            DateHelper.validate(policySupplier.get().getExpiryTime());
        }
    }

    private static void defineDeletePolicy(
        final Supplier<DeletePolicy> policySupplier,
        final Consumer<DeletePolicy> policyConsumer,
            final DeletePolicy defaultDeletePolicy
    ) throws BadRequestException
    {
        if (null == policySupplier.get()) {
            final DeletePolicy deletePolicy = cloneDeletePolicy(defaultDeletePolicy);
            policyConsumer.accept(deletePolicy);
        } else {
            DateHelper.validate(policySupplier.get().getExpiryTime());
        }
    }

    private static DeletePolicy cloneDeletePolicy(DeletePolicy deletePolicy) {
        final DeletePolicy newDeletePolicy = new DeletePolicy();
        newDeletePolicy.setOperation(deletePolicy.getOperation());
        newDeletePolicy.setExpiryTime(deletePolicy.getExpiryTime());
        return newDeletePolicy;
    }


    private static ExpirablePolicy cloneExpirablePolicy(final ExpirablePolicy expirablePolicy)
    {
        final ExpirablePolicy newExpirablePolicy = new ExpirablePolicy();
        newExpirablePolicy.setOperation(expirablePolicy.getOperation());
        newExpirablePolicy.setExpiryTime(expirablePolicy.getExpiryTime());
        return newExpirablePolicy;
    }

    private static DeletePolicy defineDefaultDeletePolicy()
    {
        final DeletePolicy deletePolicy = new DeletePolicy();
        deletePolicy.setOperation(OperationEnum.DELETE);
        deletePolicy.setExpiryTime("none");
        return deletePolicy;
    }
}
