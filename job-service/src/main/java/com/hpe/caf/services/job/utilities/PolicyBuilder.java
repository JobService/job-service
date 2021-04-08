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

public final class PolicyBuilder {

    private PolicyBuilder() {
    }

    /**
     *
     * @param job the job to be created
     * @return a map of the policies for the job
     * @throws BadRequestException if any invalid parameter
     */
    public static ExpirationPolicy buildPolicyMap(final NewJob job) throws BadRequestException {
        final ExpirationPolicy expirationPolicies;
        // creates new policies if none provided
        if (null != job.getExpiry()){
            expirationPolicies = job.getExpiry();
        }else {
            expirationPolicies = new ExpirationPolicy();
        }
        final Policy defaultPolicy = defineDefaultGlobalPolicy(expirationPolicies);
        final DeletePolicy deletePolicy = defineDefaultDeletePolicy();
        definePolicies(expirationPolicies, defaultPolicy, deletePolicy);

        return expirationPolicies;
    }

    private static void definePolicies(final ExpirationPolicy expirationPolicies, final Policy defaultPolicy,
            final DeletePolicy deletePolicy) throws BadRequestException {

        defineActivePolicy(expirationPolicies, defaultPolicy);

        defineCompletedPolicy(expirationPolicies, defaultPolicy);

        defineCancelledPolicy(expirationPolicies, defaultPolicy);

        defineFailedPolicy(expirationPolicies, defaultPolicy);

        defineWaitingPolicy(expirationPolicies, defaultPolicy);

        definePausedPolicy(expirationPolicies, defaultPolicy);

        defineExpiredPolicy(expirationPolicies, deletePolicy);
    }

    private static void defineActivePolicy(final ExpirationPolicy expirationPolicies, final Policy defaultPolicy) throws BadRequestException {
        if (null== expirationPolicies.getActive()){
            final Policy policy = duplicateDefaultPolicy(defaultPolicy);
            expirationPolicies.setActive(policy);
        }else {
            DateHelper.validate(expirationPolicies.getActive().getExpiryTime());
        }
    }

    private static void defineCancelledPolicy(final ExpirationPolicy expirationPolicies, final Policy defaultPolicy) throws BadRequestException {
        if (null== expirationPolicies.getCancelled()){
            final Policy policy = duplicateDefaultPolicy(defaultPolicy);
            expirationPolicies.setCancelled(policy);
        }else {
            DateHelper.validate(expirationPolicies.getCancelled().getExpiryTime());
        }
    }

    private static void defineCompletedPolicy(final ExpirationPolicy expirationPolicies, final Policy defaultPolicy) throws BadRequestException {
        if (null== expirationPolicies.getCompleted()){
            final Policy policy = duplicateDefaultPolicy(defaultPolicy);
            expirationPolicies.setCompleted(policy);
        }else {
            DateHelper.validate(expirationPolicies.getCompleted().getExpiryTime());
        }
    }

    private static void defineExpiredPolicy(final ExpirationPolicy expirationPolicies, final DeletePolicy deletePolicy) throws BadRequestException {
        if (null== expirationPolicies.getExpired()){
            expirationPolicies.setExpired(deletePolicy);
        }else {
            DateHelper.validate(expirationPolicies.getExpired().getExpiryTime());
        }
    }

    private static void defineFailedPolicy(final ExpirationPolicy expirationPolicies, final Policy defaultPolicy) throws BadRequestException {
        if (null== expirationPolicies.getFailed()){
            final Policy policy = duplicateDefaultPolicy(defaultPolicy);
            expirationPolicies.setFailed(policy);
        }else {
            DateHelper.validate(expirationPolicies.getFailed().getExpiryTime());
        }
    }

    private static void definePausedPolicy(final ExpirationPolicy expirationPolicies, final Policy defaultPolicy) throws BadRequestException {
        if (null== expirationPolicies.getPaused()){
            final Policy policy = duplicateDefaultPolicy(defaultPolicy);
            expirationPolicies.setPaused(policy);
        }else {
            DateHelper.validate(expirationPolicies.getPaused().getExpiryTime());
        }
    }

    private static void defineWaitingPolicy(final ExpirationPolicy expirationPolicies, final Policy defaultPolicy) throws BadRequestException {
        if (null== expirationPolicies.getWaiting()){
            final Policy policy = duplicateDefaultPolicy(defaultPolicy);
            expirationPolicies.setWaiting(policy);
        }else {
            DateHelper.validate(expirationPolicies.getWaiting().getExpiryTime());
        }
    }

    private static Policy duplicateDefaultPolicy(final Policy defaultPolicy) {
        final Policy policy = new Policy();
        policy.setOperation(defaultPolicy.getOperation());
        policy.setExpiryTime(defaultPolicy.getExpiryTime());
        return policy;
    }

    private static DeletePolicy defineDefaultDeletePolicy() {
        final DeletePolicy deletePolicy = new DeletePolicy();
        deletePolicy.setExpirationOperation(ExpirationOperationEnum.DELETE);
        deletePolicy.setExpiryTime("none");
        return deletePolicy;
    }

    /**
     *
     * @param expirationPolicies the expiration object provided
     * @return the default policy
     * @throws BadRequestException if any invalid parameter
     */
    private static Policy defineDefaultGlobalPolicy(final ExpirationPolicy expirationPolicies) throws BadRequestException {
        // define default policy
        final Policy defaultPolicy;
        if(null != expirationPolicies.getDefault()){
            defaultPolicy = expirationPolicies.getDefault();
            DateHelper.validate(defaultPolicy.getExpiryTime());
        }else {
            defaultPolicy = new Policy();
            defaultPolicy.setOperation(OperationEnum.EXPIRE);
            defaultPolicy.setExpiryTime("none");
        }
        return defaultPolicy;
    }

}
