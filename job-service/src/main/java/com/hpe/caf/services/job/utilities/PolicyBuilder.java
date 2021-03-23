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
        // create new policies if none provided
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
        if (null== expirationPolicies.getActive()) expirationPolicies.setActive(defaultPolicy);
        expirationPolicies.getActive().setExpiryTime(DateHelper.validateAndConvert(expirationPolicies.getActive().getExpiryTime()));

        if (null== expirationPolicies.getCompleted()) expirationPolicies.setCompleted(defaultPolicy);
        expirationPolicies.getCompleted().setExpiryTime(expirationPolicies.getCompleted().getExpiryTime());

        if (null== expirationPolicies.getCancelled()) expirationPolicies.setCancelled(defaultPolicy);
        expirationPolicies.getCancelled().setExpiryTime(expirationPolicies.getCancelled().getExpiryTime());

        if (null== expirationPolicies.getFailed()) expirationPolicies.setFailed(defaultPolicy);
        expirationPolicies.getFailed().setExpiryTime(expirationPolicies.getFailed().getExpiryTime());

        if (null== expirationPolicies.getWaiting()) expirationPolicies.setWaiting(defaultPolicy);
        expirationPolicies.getWaiting().setExpiryTime(expirationPolicies.getWaiting().getExpiryTime());

        if (null== expirationPolicies.getPaused()) expirationPolicies.setPaused(defaultPolicy);
        expirationPolicies.getPaused().setExpiryTime(expirationPolicies.getPaused().getExpiryTime());

        if (null== expirationPolicies.getExpired()) expirationPolicies.setExpired(deletePolicy);
        expirationPolicies.getExpired().setExpiryTime(DateHelper.validateAndConvert(expirationPolicies.getExpired().getExpiryTime()));
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
    private static Policy defineDefaultGlobalPolicy(final ExpirationPolicy expirationPolicies) {
        // define default policy
        final Policy defaultPolicy;
        if(null != expirationPolicies.getDefault()){
            defaultPolicy = expirationPolicies.getDefault();
        }else {
            defaultPolicy = new Policy();
            defaultPolicy.setOperation(OperationEnum.EXPIRE);
            defaultPolicy.setExpiryTime("none");
        }
        return defaultPolicy;
    }

}
