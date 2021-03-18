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

public final class PolicyBuilder {

    public static HashMap<String, Policy> buildPolicyMap(final NewJob job) throws BadRequestException {
        final HashMap<String, Policy> finalExpirationPolicy = new HashMap<>();
        final ExpirationPolicy expirationPolicies;
        // create new policies if none provided
        if (null != job.getExpiry()){
            expirationPolicies = job.getExpiry();
        }else {
            expirationPolicies = new ExpirationPolicy();
        }
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

        // define active policy
        final Policy activePolicy;
        if(null != expirationPolicies.getActive()){
            final String dateActive = expirationPolicies.getActive().getExpiryTime();
            DateValidator.validate(dateActive);
            activePolicy = expirationPolicies.getActive();
        }else {
            activePolicy = defaultPolicy;
        }
        finalExpirationPolicy.put("Active", activePolicy);

        // define completed policy
        final Policy completedPolicy;
        if(null != expirationPolicies.getCompleted()){
            final String dateActive = expirationPolicies.getCompleted().getExpiryTime();
            DateValidator.validate(dateActive);
            completedPolicy = expirationPolicies.getCompleted();
        }else {
            completedPolicy = defaultPolicy;
        }
        finalExpirationPolicy.put("Completed", completedPolicy);

        // define failed policy
        final Policy failedPolicy;
        if(null != expirationPolicies.getFailed()){
            final String dateActive = expirationPolicies.getFailed().getExpiryTime();
            DateValidator.validate(dateActive);
            failedPolicy = expirationPolicies.getFailed();
        }else {
            failedPolicy = defaultPolicy;
        }
        finalExpirationPolicy.put("Failed", failedPolicy);

        // define Paused policy
        final Policy pausedPolicy;
        if(null != expirationPolicies.getPaused()){
            final String dateActive = expirationPolicies.getPaused().getExpiryTime();
            DateValidator.validate(dateActive);
            pausedPolicy = expirationPolicies.getPaused();
        }else {
            pausedPolicy = defaultPolicy;
        }
        finalExpirationPolicy.put("Paused", pausedPolicy);

        // define Waiting policy
        final Policy waitingPolicy;
        if(null != expirationPolicies.getWaiting()){
            final String dateActive = expirationPolicies.getWaiting().getExpiryTime();
            DateValidator.validate(dateActive);
            waitingPolicy = expirationPolicies.getWaiting();
        }else {
            waitingPolicy = defaultPolicy;
        }
        finalExpirationPolicy.put("Waiting", waitingPolicy);

        // define Cancelled policy
        final Policy cancelledPolicy;
        if(null != expirationPolicies.getCancelled()){
            final String dateActive = expirationPolicies.getCancelled().getExpiryTime();
            DateValidator.validate(dateActive);
            cancelledPolicy = expirationPolicies.getCancelled();
        }else {
            cancelledPolicy = defaultPolicy;
        }
        finalExpirationPolicy.put("Cancelled", cancelledPolicy);

        return finalExpirationPolicy;
    }
}
