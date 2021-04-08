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

import com.hpe.caf.services.job.api.generated.model.DeletePolicy.ExpirationOperationEnum;
import com.hpe.caf.services.job.api.generated.model.ExpirationPolicy;
import com.hpe.caf.services.job.api.generated.model.Policy;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class ExpirationPolicyHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(ExpirationPolicyHelper.class);

    private ExpirationPolicyHelper()
    {
    }

    public static List<String> toDBString(final ExpirationPolicy expirationPolicy)
    {
        final List<String> policyList = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        buildPolicy(policyList, sb, "Active,", expirationPolicy.getActive());
        buildPolicy(policyList, sb, "Cancelled,", expirationPolicy.getCancelled());
        buildPolicy(policyList, sb, "Completed,", expirationPolicy.getCompleted());
        buildPolicy(policyList, sb, "Failed,", expirationPolicy.getFailed());
        buildPolicy(policyList, sb, "Waiting,", expirationPolicy.getWaiting());
        buildPolicy(policyList, sb, "Paused,", expirationPolicy.getPaused());
        sb.setLength(0);
        sb.append("(,,");
        sb.append("Expired,")
            .append(ExpirationOperationEnum.DELETE.toString()).append(",")
            .append(expirationPolicy.getExpired().getExpiryTime())
            .append(")");
        policyList.add(sb.toString());

        return policyList;
    }

    private static void buildPolicy(final List<String> policyList, final StringBuilder sb, final String status, final Policy policy)
    {
        validateExpiryTime(policy);
        sb.setLength(0);
        sb.append("(,,");
        sb.append(status)
            .append(policy.getOperation().toString()).append(",")
            .append(policy.getExpiryTime())
            .append(")");
        policyList.add(sb.toString());
    }

    private static void validateExpiryTime(final Policy policy)
    {
        final String expiryTime = policy.getExpiryTime();
        try {
            DateHelper.validate(expiryTime);
        } catch (final BadRequestException e) {
            LOG.error("invalid expiry_time {}", expiryTime);
        }
    }
}
